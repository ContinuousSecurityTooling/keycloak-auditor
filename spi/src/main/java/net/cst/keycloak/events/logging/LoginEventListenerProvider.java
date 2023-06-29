package net.cst.keycloak.events.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import net.cst.keycloak.audit.model.ClientLoginDetails;
import net.cst.keycloak.audit.model.UserLoginDetails;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static net.cst.keycloak.audit.model.Constants.*;

/**
 * Event Listener Implementation for auditing Keycloak events
 *
 * @author : mreinhardt
 * @created : 22.06.23
 **/
@Slf4j
public class LoginEventListenerProvider implements EventListenerProvider {

    private final KeycloakSession session;
    private final RealmProvider model;

    public LoginEventListenerProvider(KeycloakSession session) {
        this.session = session;
        this.model = session.realms();
    }

    @Override
    public void onEvent(Event event) {
        log.debug("Got event: {}", event.getType());
        if (EventType.LOGIN.equals(event.getType())) {
            RealmModel realm = this.model.getRealm(event.getRealmId());
            UserModel user = this.session.users().getUserById(realm, event.getUserId());

            if (user != null) {
                try {
                    log.info("Updating last login status for user: {} (client: {})", event.getUserId(), event.getClientId());
                    // Use current server time for login event
                    OffsetDateTime loginTime = OffsetDateTime.now(ZoneOffset.UTC);
                    String lastLoginAttribute = USER_EVENT_PREFIX.value() + "_" + LAST_LOGIN_INFIX.value();
                    String lastLoginDetails = user.getAttributes().get(lastLoginAttribute) != null ? user.getAttributes().get(lastLoginAttribute).get(0) : null;
                    UserLoginDetails details = getObjectMapper().readValue(lastLoginDetails != null ? lastLoginDetails : "{}", UserLoginDetails.class);
                    details.setKcLogin(loginTime);
                    details.getClientLogins().put(event.getClientId(), loginTime);
                    user.setSingleAttribute(lastLoginAttribute, getObjectMapper().writeValueAsString(details));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (EventType.CLIENT_LOGIN.equals(event.getType())) {
            RealmModel realm = this.model.getRealm(event.getRealmId());
            ClientModel client = this.session.clients().getClientByClientId(realm, event.getClientId());

            if (client != null) {
                try {
                    log.info("Updating last login status in client {} for user: {}", event.getClientId(), event.getUserId());
                    // Use current server time for login event
                    OffsetDateTime loginTime = OffsetDateTime.now(ZoneOffset.UTC);
                    String lastLoginAttribute = CLIENT_EVENT_PREFIX.value() + "_" + LAST_LOGIN_INFIX.value();
                    String lastLoginDetails = client.getAttribute(lastLoginAttribute);
                    ClientLoginDetails details = getObjectMapper().readValue(lastLoginDetails != null ? lastLoginDetails : "{}", ClientLoginDetails.class);
                    details.setKcLogin(loginTime);
                    client.setAttribute(lastLoginAttribute, getObjectMapper().writeValueAsString(details));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }

    private ObjectMapper getObjectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {

    }

    @Override
    public void close() {
        // Nothing to close
    }
}
