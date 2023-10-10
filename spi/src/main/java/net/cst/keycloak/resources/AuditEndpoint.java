package net.cst.keycloak.resources;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.cst.keycloak.audit.model.Constants.LAST_LOGIN_INFIX;
import static net.cst.keycloak.audit.model.Constants.USER_EVENT_PREFIX;

/**
 * @author : mreinhardt
 * @created : 13.07.23
 **/
@Slf4j
public class AuditEndpoint {

    private static final boolean DISABLE_EXTERNAL_ACCESS = ConfigHelper.getConfigToggle(ConfigConstants.DISABLE_EXTERNAL_ACCESS);

    private static final boolean DISABLE_ROLE_CHECK = ConfigHelper.getConfigToggle(ConfigConstants.DISABLE_ROLE_CHECK);

    private static final String ROLE_NAME = ConfigHelper.getConfigValue(ConfigConstants.DEFAULT_ROLE);

    /**
     * the current request context
     */
    @Getter(AccessLevel.PROTECTED)
    private final KeycloakSession keycloakSession;
    private final AccessToken auth;

    public AuditEndpoint(KeycloakSession keycloakSession) {
        this.keycloakSession = keycloakSession;
        this.auth = Tokens.getAccessToken(keycloakSession);
        new AppAuthManager.BearerTokenAuthenticator(keycloakSession).authenticate();
    }

    @Path("users")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuditedUserRepresentation> listUsers(@Context HttpHeaders headers) {
        this.checkAccessRights(headers);
        String realmName = auth.getIssuer().substring(auth.getIssuer().lastIndexOf('/') + 1);
        RealmManager realmManager = new RealmManager(this.keycloakSession);
        RealmModel realm = realmManager.getRealmByName(realmName);
        log.debug("Checking for users in realm {}", realmName);
        List<UserModel> users = this.keycloakSession.users().searchForUserStream(realm, "*").toList();
        log.debug("Got {} users", (long) users.size());
        return users.stream().map(AuditEndpoint::toBriefRepresentation).toList();
    }

    @Path("clients")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuditedClientRepresentation> listClients(@Context HttpHeaders headers) {
        this.checkAccessRights(headers);
        String realmName = auth.getIssuer().substring(auth.getIssuer().lastIndexOf('/') + 1);
        RealmManager realmManager = new RealmManager(this.keycloakSession);
        RealmModel realm = realmManager.getRealmByName(realmName);
        log.debug("Checking for clients in realm {}", realmName);
        List<ClientModel> clients = this.keycloakSession.clients().getClientsStream(realm).toList();
        log.debug("Got {} clients", (long) clients.size());
        return clients.stream().map(clientModel -> AuditEndpoint.toBriefRepresentation(clientModel, keycloakSession)).toList();
    }

    protected void checkAccessRights(HttpHeaders headers) {
        if (DISABLE_EXTERNAL_ACCESS) {
            if (!headers.getRequestHeader("x-forwarded-host").isEmpty()) {
                log.info("No external access allowed");
                throw new ForbiddenException();
            }
        }
        if (this.auth == null) {
            log.error("Empty authentication details");
            throw new NotAuthorizedException("Bearer");
        } else if (!DISABLE_ROLE_CHECK &&
                this.auth.getRealmAccess() == null && this.auth.getRealmAccess().isUserInRole(ROLE_NAME)) {
            log.error("No access to realm with auth {}", this.auth);
            throw new ForbiddenException("Don't have realm access");
        }
        log.debug("Got user with id {}", this.auth.getId());
    }

    public static AuditedUserRepresentation toBriefRepresentation(UserModel user) {
        AuditedUserRepresentation rep = new AuditedUserRepresentation();
        BeanCopy.from(ModelToRepresentation.toBriefRepresentation(user)).to(rep).copy();

        String lastLoginAttribute = USER_EVENT_PREFIX.value() + "_" + LAST_LOGIN_INFIX.value();
        if (user.getAttributes() != null && user.getAttributes().get(lastLoginAttribute) != null) {
            rep.setLastLogin(user.getAttributes().get(lastLoginAttribute).get(0));
            // check client logins
            List<String> clients = user.getAttributes().keySet().stream().filter(key -> key.startsWith(lastLoginAttribute + "_")).toList();
            for (String client : clients) {
                String clientName = client.split(lastLoginAttribute + "_")[1];
                rep.getClientLogins().put(clientName, user.getAttributes().get(client).get(0));
            }
            log.debug("Got clients {}", clients);
        } else {
            rep.setLastLogin(null);
            rep.setClientLogins(null);
        }
        return rep;
    }

    public static AuditedClientRepresentation toBriefRepresentation(ClientModel client, KeycloakSession session) {
        AuditedClientRepresentation rep = new AuditedClientRepresentation();
        BeanCopy.from(ModelToRepresentation.toRepresentation(client, session)).to(rep).copy();

        String lastLoginAttribute = USER_EVENT_PREFIX.value() + "_" + LAST_LOGIN_INFIX.value();
        if (client.getAttributes() != null && client.getAttributes().get(lastLoginAttribute) != null) {
            rep.setLastLogin(client.getAttributes().get(lastLoginAttribute));
        } else {
            rep.setLastLogin(null);
        }
        return rep;
    }
}
