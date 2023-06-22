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
    private final static String USER_EVENT_PREFIX = "aud_usr";
    private final static String CLIENT_EVENT_PREFIX = "aud_cls";
    private final static String ADMIN_EVENT_PREFIX = "aud_adm";

    private final static String LAST_LOGIN_INFIX = "last-login";

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
                String loginTimeS = DateTimeFormatter.ISO_DATE_TIME.format(loginTime);
                user.setSingleAttribute(USER_EVENT_PREFIX + "_" + LAST_LOGIN_INFIX + "_" + event.getClientId(), loginTimeS);
            }
        }
        if (EventType.CLIENT_LOGIN.equals(event.getType())) {
            RealmModel realm = this.model.getRealm(event.getRealmId());
            ClientModel client = this.session.clients().getClientByClientId(realm, event.getClientId());

            if (client != null) {
                log.info("Updating last login status in client {} for user: {}", event.getClientId(), event.getUserId());
                // Use current server time for login event
                OffsetDateTime loginTime = OffsetDateTime.now(ZoneOffset.UTC);
                String loginTimeS = DateTimeFormatter.ISO_DATE_TIME.format(loginTime);
                client.setAttribute(CLIENT_EVENT_PREFIX + "_" + LAST_LOGIN_INFIX, loginTimeS);
            }
        }

    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {

    }

    @Override
    public void close() {
        // Nothing to close
    }
}
