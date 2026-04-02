package net.cst.keycloak.resources;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jodd.bean.BeanCopy;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.cst.keycloak.audit.model.AuditedClientRepresentation;
import net.cst.keycloak.audit.model.AuditedUserRepresentation;
import net.cst.keycloak.audit.model.ConfigConstants;
import net.cst.keycloak.utils.ConfigHelper;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.RealmManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.cst.keycloak.audit.model.Constants.LAST_LOGIN_INFIX;
import static net.cst.keycloak.audit.model.Constants.USER_EVENT_PREFIX;

@Slf4j
public class AuditEndpoint {

    private final boolean disableExternalAccess;

    private final boolean disableRoleCheck;

    private final boolean globalMasterAccess;

    private final String roleName;

    /**
     * the current request context
     */
    @Getter(AccessLevel.PROTECTED)
    private final KeycloakSession keycloakSession;
    private final AccessToken auth;

    public AuditEndpoint(KeycloakSession keycloakSession) {
        this.keycloakSession = keycloakSession;
        this.auth = Tokens.getAccessToken(this.keycloakSession);
        disableExternalAccess = ConfigHelper.getConfigToggle(ConfigConstants.DISABLE_EXTERNAL_ACCESS);
        disableRoleCheck = ConfigHelper.getConfigToggle(ConfigConstants.DISABLE_ROLE_CHECK);
        globalMasterAccess = ConfigHelper.getConfigToggle(ConfigConstants.GLOBAL_MASTER_ACCESS);
        roleName = ConfigHelper.getConfigValue(ConfigConstants.DEFAULT_ROLE);
    }

    public static AuditedUserRepresentation toBriefRepresentation(UserModel user, String realm) {
        AuditedUserRepresentation rep = new AuditedUserRepresentation();
        BeanCopy.from(ModelToRepresentation.toBriefRepresentation(user)).to(rep).copy();
        rep.setRealm(realm);

        String lastLoginAttribute = USER_EVENT_PREFIX.value() + "_" + LAST_LOGIN_INFIX.value();
        if (user.getAttributes() != null && user.getAttributes().get(lastLoginAttribute) != null) {
            rep.setLastLogin(user.getAttributes().get(lastLoginAttribute).get(0));
            // check client logins
            List<String> clients = user.getAttributes().keySet().stream()
                    .filter(key -> key.startsWith(lastLoginAttribute + "_")).toList();
            for (String client : clients) {
                String clientName = client.split(lastLoginAttribute + "_")[1];
                rep.getClientLogins().put(clientName, user.getAttributes().get(client).get(0));
            }
            log.debug("Got {} clients for user {}", clients.size(), user.getId());
        } else {
            rep.setLastLogin(null);
            rep.setClientLogins(null);
        }
        return rep;
    }

    public static AuditedClientRepresentation toBriefRepresentation(ClientModel client, String realm,
                                                                    KeycloakSession session) {
        AuditedClientRepresentation rep = new AuditedClientRepresentation();
        BeanCopy.from(ModelToRepresentation.toRepresentation(client, session)).to(rep).copy();
        rep.setRealm(realm);

        String lastLoginAttribute = USER_EVENT_PREFIX.value() + "_" + LAST_LOGIN_INFIX.value();
        if (client.getAttributes() != null && client.getAttributes().get(lastLoginAttribute) != null) {
            rep.setLastLogin(client.getAttributes().get(lastLoginAttribute));
        } else {
            rep.setLastLogin(null);
        }
        return rep;
    }

    public void authenticate() {
        new AppAuthManager.BearerTokenAuthenticator(keycloakSession).authenticate();
    }

    @Path("users")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuditedUserRepresentation> listUsers(@Context HttpHeaders headers,
                                                     @QueryParam("scope") String scope,
                                                     @QueryParam("realm") String realmFilter) {
        this.checkAccessRights(headers);
        String realmName = auth.getIssuer().substring(auth.getIssuer().lastIndexOf('/') + 1);
        RealmManager realmManager = new RealmManager(this.keycloakSession);
        List<AuditedUserRepresentation> users = new ArrayList<>();
        if (shouldIncludeAllRealms(realmName, scope)) {
            if (realmFilter != null && !realmFilter.isBlank()) {
                RealmModel targetRealm = realmManager.getSession().realms().getRealmByName(realmFilter);
                if (targetRealm != null) {
                    users.addAll(readUsers(targetRealm).stream()
                            .map(u -> AuditEndpoint.toBriefRepresentation(u, targetRealm.getName())).toList());
                    log.debug("Adding user info filtered to realm {}", realmFilter);
                }
            } else {
                realmManager.getSession().realms().getRealmsStream().forEach(realm -> users.addAll(readUsers(realm).stream()
                        .map(userModel -> AuditEndpoint.toBriefRepresentation(userModel, realm.getName())).toList()));
                log.debug("Adding user info for all realms");
            }
        } else {
            users.addAll(readUsers(realmManager.getRealmByName(realmName)).stream()
                    .map(userModel -> AuditEndpoint.toBriefRepresentation(userModel, realmName)).toList());
            log.debug("Adding user info in realm {}", realmName);
        }
        return users;
    }

    private List<UserModel> readUsers(RealmModel realm) {
        log.debug("Checking for users in realm {}", realm.getName());
        final List<UserModel> users = this.keycloakSession.users()
                .searchForUserStream(realm, Map.of(UserModel.SEARCH, "*")).toList();
        log.debug("Got {} users", (long) users.size());
        return users;
    }

    @Path("clients")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuditedClientRepresentation> listClients(@Context HttpHeaders headers,
                                                         @QueryParam("scope") String scope,
                                                         @QueryParam("realm") String realmFilter) {
        this.checkAccessRights(headers);
        String realmName = auth.getIssuer().substring(auth.getIssuer().lastIndexOf('/') + 1);
        RealmManager realmManager = new RealmManager(this.keycloakSession);
        List<AuditedClientRepresentation> clients = new ArrayList<>();
        if (shouldIncludeAllRealms(realmName, scope)) {
            if (realmFilter != null && !realmFilter.isBlank()) {
                RealmModel targetRealm = realmManager.getSession().realms().getRealmByName(realmFilter);
                if (targetRealm != null) {
                    clients.addAll(readClients(targetRealm).stream()
                            .map(c -> AuditEndpoint.toBriefRepresentation(c, targetRealm.getName(), keycloakSession))
                            .toList());
                    log.debug("Adding client info filtered to realm {}", realmFilter);
                }
            } else {
                realmManager.getSession().realms().getRealmsStream().forEach(realm -> clients.addAll(readClients(realm)
                        .stream()
                        .map(clientModel -> AuditEndpoint.toBriefRepresentation(clientModel, realm.getName(), keycloakSession))
                        .toList()));
                log.debug("Adding client info for all realms");
            }
        } else {
            clients.addAll(readClients(realmManager.getRealmByName(realmName)).stream()
                    .map(clientModel -> AuditEndpoint.toBriefRepresentation(clientModel, realmName, keycloakSession))
                    .toList());
            log.debug("Adding client info in realm {}", realmName);
        }
        return clients;
    }

    private boolean shouldIncludeAllRealms(String realmName, String scope) {
        if ("current-realm".equals(scope)) return false;
        return globalMasterAccess || "master".equals(realmName);
    }

    @Path("users/csv")
    @GET
    @Produces("text/csv")
    public Response downloadUsersCsv(@Context HttpHeaders headers,
                                     @QueryParam("scope") String scope,
                                     @QueryParam("realm") String realmFilter) {
        List<AuditedUserRepresentation> users = listUsers(headers, scope, realmFilter);
        StringBuilder csv = new StringBuilder("username,email,firstName,lastName,realm,lastLogin\n");
        for (AuditedUserRepresentation u : users) {
            csv.append(escapeCsv(u.getUsername())).append(",")
               .append(escapeCsv(u.getEmail())).append(",")
               .append(escapeCsv(u.getFirstName())).append(",")
               .append(escapeCsv(u.getLastName())).append(",")
               .append(escapeCsv(u.getRealm())).append(",")
               .append(escapeCsv(u.getLastLogin())).append("\n");
        }
        return Response.ok(csv.toString())
                .header("Content-Disposition", "attachment; filename=\"audit-users-report.csv\"")
                .build();
    }

    @Path("clients/csv")
    @GET
    @Produces("text/csv")
    public Response downloadClientsCsv(@Context HttpHeaders headers,
                                       @QueryParam("scope") String scope,
                                       @QueryParam("realm") String realmFilter) {
        List<AuditedClientRepresentation> clients = listClients(headers, scope, realmFilter);
        StringBuilder csv = new StringBuilder("clientId,name,realm,lastLogin\n");
        for (AuditedClientRepresentation c : clients) {
            csv.append(escapeCsv(c.getClientId())).append(",")
               .append(escapeCsv(c.getName())).append(",")
               .append(escapeCsv(c.getRealm())).append(",")
               .append(escapeCsv(c.getLastLogin())).append("\n");
        }
        return Response.ok(csv.toString())
                .header("Content-Disposition", "attachment; filename=\"audit-clients-report.csv\"")
                .build();
    }

    @Path("download")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response downloadPage() {
        RealmModel currentRealm = keycloakSession.getContext().getRealm();
        String currentRealmName = currentRealm != null ? currentRealm.getName() : "unknown";
        boolean isMasterRealm = "master".equals(currentRealmName);

        StringBuilder realmRows = new StringBuilder();
        if (isMasterRealm) {
            realmRows.append(realmRow("All Realms", "all", true));
            keycloakSession.realms().getRealmsStream()
                    .map(RealmModel::getName)
                    .sorted()
                    .forEach(name -> realmRows.append(realmRow(name, name, false)));
        } else {
            realmRows.append(realmRow(currentRealmName, currentRealmName, false));
        }

        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <title>Audit Reporting</title>
                  <style>
                    body { font-family: sans-serif; max-width: 860px; margin: 40px auto; padding: 0 20px; color: #333; }
                    h1 { border-bottom: 2px solid #e00; padding-bottom: 8px; }
                    h2 { margin-top: 28px; }
                    .field-row { display: flex; align-items: center; gap: 8px; margin: 12px 0; }
                    .field-row label { min-width: 110px; font-size: 14px; }
                    .field-row input { flex: 1; padding: 6px 8px; border: 1px solid #ccc; border-radius: 4px; font-size: 13px; }
                    .field-row button { padding: 6px 12px; border: 1px solid #aaa; border-radius: 4px; cursor: pointer; background: #f5f5f5; }
                    .hint { font-size: 12px; color: #666; margin-top: 4px; }
                    table { border-collapse: collapse; width: 100%; margin-top: 16px; }
                    th { background: #f0f0f0; text-align: left; padding: 8px 12px; border: 1px solid #ddd; font-size: 13px; }
                    td { padding: 7px 12px; border: 1px solid #ddd; font-size: 13px; vertical-align: middle; }
                    tr:first-child td { font-weight: bold; background: #fafafa; }
                    .btn { display: inline-block; padding: 4px 10px; border: none; border-radius: 3px; cursor: pointer; font-size: 12px; white-space: nowrap; }
                    .btn-json { background: #0066cc; color: white; }
                    .btn-csv  { background: #217346; color: white; }
                    #status { margin-top: 16px; color: #c00; }
                  </style>
                </head>
                <body>
                  <h1>Audit Reporting</h1>

                  <h2>Authentication</h2>
                  <div class="field-row">
                    <label for="token">Bearer Token</label>
                    <input type="password" id="token" placeholder="Paste your Admin Bearer token here" />
                    <button onclick="autoDetect()">Auto-detect</button>
                  </div>
                  <p class="hint">
                    Obtain your token via:
                    <code>curl -s -d 'client_id=admin-cli&amp;username=admin&amp;password=&lt;pw&gt;&amp;grant_type=password'
                    .../realms/master/protocol/openid-connect/token | jq -r .access_token</code>
                  </p>

                  <h2>Reports</h2>
                  <table>
                    <thead>
                      <tr>
                        <th>Realm</th>
                        <th>Users JSON</th>
                        <th>Users CSV</th>
                        <th>Clients JSON</th>
                        <th>Clients CSV</th>
                      </tr>
                    </thead>
                    <tbody>
                """ + realmRows + """
                    </tbody>
                  </table>

                  <div id="status"></div>

                  <script>
                    const base = window.location.href.replace(/\\/download$/, '');

                    function autoDetect() {
                      let found = null;
                      for (const key of Object.keys(sessionStorage)) {
                        const val = sessionStorage.getItem(key);
                        if (val && val.split('.').length === 3 && val.length > 100) {
                          found = val; break;
                        }
                      }
                      if (!found && window.keycloak) found = window.keycloak.token;
                      if (found) {
                        document.getElementById('token').value = found;
                        document.getElementById('status').textContent = 'Token detected.';
                      } else {
                        document.getElementById('status').textContent = 'Could not auto-detect token \u2014 please paste it manually.';
                      }
                    }

                    async function dl(type, fmt, filename, realm) {
                      const token = document.getElementById('token').value.trim();
                      if (!token) { document.getElementById('status').textContent = 'Please provide a Bearer token.'; return; }
                      const path = fmt === 'csv' ? `${base}/${type}/csv` : `${base}/${type}`;
                      const params = new URLSearchParams();
                      if (realm === 'all') params.set('scope', 'all-realms');
                      else params.set('realm', realm);
                      const url = path + '?' + params.toString();
                      const accept = fmt === 'csv' ? 'text/csv' : 'application/json';
                      document.getElementById('status').textContent = 'Downloading\u2026';
                      try {
                        const resp = await fetch(url, { headers: { Authorization: 'Bearer ' + token, Accept: accept } });
                        if (!resp.ok) { document.getElementById('status').textContent = 'Error ' + resp.status + ': ' + await resp.text(); return; }
                        const blob = await resp.blob();
                        const a = Object.assign(document.createElement('a'), { href: URL.createObjectURL(blob), download: filename });
                        a.click(); URL.revokeObjectURL(a.href);
                        document.getElementById('status').textContent = '';
                      } catch (e) { document.getElementById('status').textContent = 'Download failed: ' + e; }
                    }
                  </script>
                </body>
                </html>
                """;
        return Response.ok(html).build();
    }

    private static String realmRow(String label, String realmParam, boolean isAll) {
        String uFile = isAll ? "audit-users-all" : "audit-users-" + realmParam;
        String cFile = isAll ? "audit-clients-all" : "audit-clients-" + realmParam;
        return "      <tr>\n"
                + "        <td>" + label + "</td>\n"
                + "        <td><button class=\"btn btn-json\" onclick=\"dl('users','json','" + uFile + ".json','" + realmParam + "')\">&#8595; JSON</button></td>\n"
                + "        <td><button class=\"btn btn-csv\"  onclick=\"dl('users','csv','" + uFile + ".csv','" + realmParam + "')\">&#8595; CSV</button></td>\n"
                + "        <td><button class=\"btn btn-json\" onclick=\"dl('clients','json','" + cFile + ".json','" + realmParam + "')\">&#8595; JSON</button></td>\n"
                + "        <td><button class=\"btn btn-csv\"  onclick=\"dl('clients','csv','" + cFile + ".csv','" + realmParam + "')\">&#8595; CSV</button></td>\n"
                + "      </tr>\n";
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private List<ClientModel> readClients(RealmModel realm) {
        log.debug("Checking for clients in realm {}", realm.getName());
        List<ClientModel> clients = this.keycloakSession.clients().getClientsStream(realm).toList();
        log.debug("Got {} clients", (long) clients.size());
        return clients;
    }

    protected void checkAccessRights(HttpHeaders headers) {
        this.authenticate();

        if (disableExternalAccess && !headers.getRequestHeader("x-forwarded-host").isEmpty()) {
            log.error("No external access allowed");
            throw new ForbiddenException();
        }

        if (this.auth == null) {
            log.error("Empty authentication details");
            throw new NotAuthorizedException("Bearer");
        } else if (!disableRoleCheck && (
                this.auth.getRealmAccess() == null || !this.auth.getRealmAccess().isUserInRole(roleName)
        )) {
            log.error("No access to realm with auth {}", this.auth);
            throw new ForbiddenException("Don't have realm access");
        }
        log.debug("Got user with id {}", this.auth.getId());
    }
}
