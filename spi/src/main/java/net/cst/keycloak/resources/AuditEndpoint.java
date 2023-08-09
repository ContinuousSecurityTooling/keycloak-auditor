package net.cst.keycloak.resources;

import jodd.bean.BeanCopy;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.cst.keycloak.audit.model.AuditedClientRepresentation;
import net.cst.keycloak.audit.model.AuditedUserRepresentation;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.RealmManager;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

import static net.cst.keycloak.audit.model.Constants.LAST_LOGIN_INFIX;
import static net.cst.keycloak.audit.model.Constants.USER_EVENT_PREFIX;

/**
 * @author : mreinhardt
 * @created : 13.07.23
 **/
@Slf4j
public class AuditEndpoint {
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
    public List<AuditedUserRepresentation> listUsers() {
        this.checkAccessRights();
        String realmName = auth.getIssuer().substring(auth.getIssuer().lastIndexOf('/') + 1);
        RealmManager realmManager = new RealmManager(this.keycloakSession);
        RealmModel realm = realmManager.getRealmByName(realmName);
        log.debug("Checking for users in realm {}", realmName);
        List<UserModel> users = this.keycloakSession.users().searchForUserStream(realm, "*")
                .collect(Collectors.toList());
        log.debug("Got {} users", (long) users.size());
        return users.stream().map(AuditEndpoint::toBriefRepresentation).collect(Collectors.toList());
    }

    @Path("clients")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuditedClientRepresentation> listClients() {
        this.checkAccessRights();
        String realmName = auth.getIssuer().substring(auth.getIssuer().lastIndexOf('/') + 1);
        RealmManager realmManager = new RealmManager(this.keycloakSession);
        RealmModel realm = realmManager.getRealmByName(realmName);
        log.debug("Checking for clients in realm {}", realmName);
        List<ClientModel> clients = this.keycloakSession.clients().getClientsStream(realm)
                .collect(Collectors.toList());
        log.debug("Got {} clients", (long) clients.size());
        return clients.stream().map(clientModel -> AuditEndpoint.toBriefRepresentation(clientModel, keycloakSession)).
                collect(Collectors.toList());
    }

    protected void checkAccessRights() {
        if (this.auth == null) {
            log.error("Empty authentication details");
            throw new NotAuthorizedException("Bearer");
        } else if (this.auth.getRealmAccess() == null) {
            log.error("No access to realm");
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
            List<String> clients = user.getAttributes().keySet().stream().filter(key -> key.startsWith(lastLoginAttribute + "_")).collect(Collectors.toList());
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
