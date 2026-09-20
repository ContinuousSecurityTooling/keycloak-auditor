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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.servers.ServerVariable;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;

import static net.cst.keycloak.audit.model.Constants.LAST_LOGIN_INFIX;
import static net.cst.keycloak.audit.model.Constants.USER_EVENT_PREFIX;

@Slf4j
@Tag(name = "Auditing", description = "Query last-login audit data for users and clients")
@Server(url = "{keycloakBaseUrl}/realms/{realm}", variables = {
        @ServerVariable(name = "keycloakBaseUrl", defaultValue = "http://localhost:8080",
                description = "Base URL of the Keycloak server"),
        @ServerVariable(name = "realm", defaultValue = "master",
                description = "Realm whose auditing resource is being called")
})
@SecurityScheme(securitySchemeName = "bearerAuth", type = SecuritySchemeType.HTTP,
        scheme = "bearer", bearerFormat = "JWT",
        description = "Keycloak admin access token. Unless KC_AUD_DISABLE_ROLE_CHECK=true, the token must carry "
                + "the realm role configured via KC_AUD_DEFAULT_ROLE (default: view-users).")
public class AuditEndpoint {

    /**
     * Sent by the download page's own JS to opt in to falling back to the caller's existing
     * Keycloak SSO session cookie when no Bearer token is supplied. Only same-origin requests
     * can set a custom header without tripping CORS, so this also keeps the fallback from being
     * triggered by simple cross-site requests (forms, images, top-level navigation).
     */
    private static final String SESSION_AUTH_HEADER = "X-Audit-Use-Session";

    private final boolean disableExternalAccess;

    private final boolean disableRoleCheck;

    private final boolean globalMasterAccess;

    private final String roleName;

    /**
     * the current request context
     */
    @Getter(AccessLevel.PROTECTED)
    private final KeycloakSession keycloakSession;
    private AccessToken auth;

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
    @Operation(operationId = "listUsers", summary = "List audited users",
            description = "Returns users with their global and per-client last-login timestamps. Non-master callers "
                    + "only see their own realm unless KC_AUD_GLOBAL_MASTER_ACCESS=true; master-realm tokens are "
                    + "auto-scoped to all realms.")
    @SecurityRequirement(name = "bearerAuth")
    @APIResponse(responseCode = "200", description = "Audited users",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = SchemaType.ARRAY, implementation = AuditedUserRepresentation.class)))
    @APIResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @APIResponse(responseCode = "403", description = "Token lacks the required realm role, or external access is disabled")
    public List<AuditedUserRepresentation> listUsers(@Context HttpHeaders headers,
                                                     @Parameter(description = "Use `current-realm` to force "
                                                             + "single-realm results even for master tokens. Omit or "
                                                             + "use `all-realms` for the default behaviour.")
                                                     @QueryParam("scope") String scope,
                                                     @Parameter(description = "When the caller has all-realm access, "
                                                             + "restrict results to this realm only.")
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
    @Operation(operationId = "listClients", summary = "List audited clients",
            description = "Returns clients with their last-login timestamp. Scoping rules match `listUsers`.")
    @SecurityRequirement(name = "bearerAuth")
    @APIResponse(responseCode = "200", description = "Audited clients",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = SchemaType.ARRAY, implementation = AuditedClientRepresentation.class)))
    @APIResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @APIResponse(responseCode = "403", description = "Token lacks the required realm role, or external access is disabled")
    public List<AuditedClientRepresentation> listClients(@Context HttpHeaders headers,
                                                         @Parameter(description = "Use `current-realm` to force "
                                                                 + "single-realm results even for master tokens.")
                                                         @QueryParam("scope") String scope,
                                                         @Parameter(description = "When the caller has all-realm "
                                                                 + "access, restrict results to this realm only.")
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
    @Operation(operationId = "downloadUsersCsv", summary = "Download audited users as CSV",
            description = "Same data as `listUsers`, returned as a CSV attachment.")
    @SecurityRequirement(name = "bearerAuth")
    @APIResponse(responseCode = "200", description = "CSV report (Content-Disposition: attachment)",
            content = @Content(mediaType = "text/csv", schema = @Schema(type = SchemaType.STRING, format = "binary")))
    @APIResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @APIResponse(responseCode = "403", description = "Token lacks the required realm role, or external access is disabled")
    public Response downloadUsersCsv(@Context HttpHeaders headers,
                                     @Parameter(description = "Use `current-realm` to force single-realm results.")
                                     @QueryParam("scope") String scope,
                                     @Parameter(description = "Restrict results to this realm only.")
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
    @Operation(operationId = "downloadClientsCsv", summary = "Download audited clients as CSV",
            description = "Same data as `listClients`, returned as a CSV attachment.")
    @SecurityRequirement(name = "bearerAuth")
    @APIResponse(responseCode = "200", description = "CSV report (Content-Disposition: attachment)",
            content = @Content(mediaType = "text/csv", schema = @Schema(type = SchemaType.STRING, format = "binary")))
    @APIResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @APIResponse(responseCode = "403", description = "Token lacks the required realm role, or external access is disabled")
    public Response downloadClientsCsv(@Context HttpHeaders headers,
                                       @Parameter(description = "Use `current-realm` to force single-realm results.")
                                       @QueryParam("scope") String scope,
                                       @Parameter(description = "Restrict results to this realm only.")
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
    @Operation(operationId = "downloadPage", summary = "Audit reporting download page",
            description = "Self-contained HTML page with buttons that call the JSON/CSV endpoints. Served without "
                    + "authentication; the page itself prompts for a bearer token.")
    @APIResponse(responseCode = "200", description = "HTML page",
            content = @Content(mediaType = MediaType.TEXT_HTML, schema = @Schema(type = SchemaType.STRING)))
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
                    <input type="password" id="token" placeholder="Optional if you're already logged into this admin console" />
                  </div>
                  <p class="hint">
                    If you're logged into the Keycloak admin console in this browser, downloads below will use that
                    session automatically — no token needed. Otherwise, paste an Admin Bearer token, obtained via:
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
                    const SESSION_HEADER = 'X-Audit-Use-Session';

                    function authHeaders(accept) {
                      const token = document.getElementById('token').value.trim();
                      const headers = { Accept: accept, [SESSION_HEADER]: '1' };
                      if (token) headers.Authorization = 'Bearer ' + token;
                      return headers;
                    }

                    async function checkSession() {
                      try {
                        const resp = await fetch(`${base}/users?scope=current-realm`, { headers: authHeaders('application/json') });
                        const status = document.getElementById('status');
                        if (resp.ok) {
                          status.style.color = '#080';
                          status.textContent = 'Using your current Keycloak admin session \u2014 no token needed.';
                        }
                      } catch (e) { /* ignore; user can still paste a token */ }
                    }
                    checkSession();

                    async function dl(type, fmt, filename, realm) {
                      const path = fmt === 'csv' ? `${base}/${type}/csv` : `${base}/${type}`;
                      const params = new URLSearchParams();
                      if (realm === 'all') params.set('scope', 'all-realms');
                      else params.set('realm', realm);
                      const url = path + '?' + params.toString();
                      const accept = fmt === 'csv' ? 'text/csv' : 'application/json';
                      const status = document.getElementById('status');
                      status.style.color = '#c00';
                      status.textContent = 'Downloading\u2026';
                      try {
                        const resp = await fetch(url, { headers: authHeaders(accept) });
                        if (!resp.ok) {
                          status.textContent = resp.status === 401
                            ? 'Not signed in \u2014 please paste a Bearer token above or log into the admin console in this browser.'
                            : 'Error ' + resp.status + ': ' + await resp.text();
                          return;
                        }
                        const blob = await resp.blob();
                        const a = Object.assign(document.createElement('a'), { href: URL.createObjectURL(blob), download: filename });
                        a.click(); URL.revokeObjectURL(a.href);
                        status.textContent = '';
                      } catch (e) { status.textContent = 'Download failed: ' + e; }
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

    /**
     * Characters that Excel/LibreOffice/Sheets treat as the start of a formula (or, for
     * tab/CR, as a way to smuggle one past naive delimiter checks) when a cell begins with
     * them. Left unescaped, a user-controlled field (e.g. firstName) containing a payload like
     * {@code =HYPERLINK("http://evil/",A1)} would execute as a live formula the moment an
     * admin opens the exported report — see OWASP's CSV Injection guidance.
     */
    private static final Set<Character> CSV_FORMULA_TRIGGERS = Set.of('=', '+', '-', '@', '\t', '\r');

    private String escapeCsv(String value) {
        if (value == null) return "";
        String sanitized = value;
        if (!sanitized.isEmpty() && CSV_FORMULA_TRIGGERS.contains(sanitized.charAt(0))) {
            sanitized = "'" + sanitized;
        }
        if (sanitized.contains(",") || sanitized.contains("\"") || sanitized.contains("\n")) {
            return "\"" + sanitized.replace("\"", "\"\"") + "\"";
        }
        return sanitized;
    }

    private List<ClientModel> readClients(RealmModel realm) {
        log.debug("Checking for clients in realm {}", realm.getName());
        List<ClientModel> clients = this.keycloakSession.clients().getClientsStream(realm).toList();
        log.debug("Got {} clients", (long) clients.size());
        return clients;
    }

    protected void checkAccessRights(HttpHeaders headers) {
        this.authenticate();

        UserModel sessionCookieUser = null;
        if (this.auth == null) {
            AuthenticationManager.AuthResult sessionAuth = authenticateViaSessionCookie(headers);
            if (sessionAuth != null) {
                this.auth = sessionAuth.getToken();
                sessionCookieUser = sessionAuth.getUser();
            }
        }

        if (disableExternalAccess && !headers.getRequestHeader("x-forwarded-host").isEmpty()) {
            log.error("No external access allowed");
            throw new ForbiddenException();
        }

        if (this.auth == null) {
            log.error("Empty authentication details");
            throw new NotAuthorizedException("Bearer");
        } else if (!disableRoleCheck && !hasRequiredRole(sessionCookieUser)) {
            log.error("No access to realm with auth {}", this.auth);
            throw new ForbiddenException("Don't have realm access");
        }
        log.debug("Got user with id {}", this.auth.getId());
    }

    /**
     * Falls back to the caller's existing Keycloak SSO session (the KEYCLOAK_IDENTITY cookie set
     * when they're already logged into the admin console in this browser) so the download page
     * doesn't require pasting a Bearer token by hand. Only attempted when the request opts in via
     * {@link #SESSION_AUTH_HEADER}; the identity cookie itself carries no realm roles, so the role
     * check for this path is done separately against the resolved user in {@link #hasRequiredRole}.
     */
    private AuthenticationManager.AuthResult authenticateViaSessionCookie(HttpHeaders headers) {
        List<String> marker = headers.getRequestHeader(SESSION_AUTH_HEADER);
        if (marker == null || marker.isEmpty()) {
            return null;
        }
        RealmModel realm = keycloakSession.getContext().getRealm();
        if (realm == null) {
            return null;
        }
        AuthenticationManager.AuthResult result = AuthenticationManager.authenticateIdentityCookie(keycloakSession, realm, true);
        if (result != null) {
            log.debug("Authenticated via existing Keycloak session cookie for user {}", result.getUser().getId());
        }
        return result;
    }

    private boolean hasRequiredRole(UserModel sessionCookieUser) {
        if (sessionCookieUser != null) {
            RealmModel realm = keycloakSession.getContext().getRealm();
            RoleModel role = realm != null ? realm.getRole(roleName) : null;
            return role != null && sessionCookieUser.hasRole(role);
        }
        return this.auth.getRealmAccess() != null && this.auth.getRealmAccess().isUserInRole(roleName);
    }
}
