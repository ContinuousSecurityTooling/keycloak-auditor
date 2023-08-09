package net.cst.keycloak.events.logging;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

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
                log.info("Updating last login status for user: {} (client: {})", event.getUserId(), event.getClientId());
                // Use current server time for login event
                OffsetDateTime loginTime = OffsetDateTime.now(ZoneOffset.UTC);
                String lastLoginAttribute = USER_EVENT_PREFIX.value() + "_" + LAST_LOGIN_INFIX.value();
                String loginTimeS = DateTimeFormatter.ISO_DATE_TIME.format(loginTime);
                user.setSingleAttribute(lastLoginAttribute, loginTimeS);
                user.setSingleAttribute(lastLoginAttribute + "_" + event.getClientId(), loginTimeS);
            }
        }
        if (EventType.CLIENT_LOGIN.equals(event.getType())) {
            RealmModel realm = this.model.getRealm(event.getRealmId());
            ClientModel client = this.session.clients().getClientByClientId(realm, event.getClientId());

            if (client != null) {
                log.info("Updating last login status in client {} for user: {}", event.getClientId(), event.getUserId());
                // Use current server time for login event
                OffsetDateTime loginTime = OffsetDateTime.now(ZoneOffset.UTC);
                String lastLoginAttribute = CLIENT_EVENT_PREFIX.value() + "_" + LAST_LOGIN_INFIX.value();
                String loginTimeS = DateTimeFormatter.ISO_DATE_TIME.format(loginTime);
                client.setAttribute(lastLoginAttribute, loginTimeS);
            }
        }

    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
        // Nothing to do
    }

    @Override
    public void close() {
        // Nothing to close
    }
}
