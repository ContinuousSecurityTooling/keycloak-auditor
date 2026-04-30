package net.cst.keycloak.events.logging;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import net.cst.keycloak.audit.model.ConfigConstants;
import net.cst.keycloak.userprofile.AuditUserProfileRegistrar;
import net.cst.keycloak.utils.ConfigHelper;

import static net.cst.keycloak.audit.model.Constants.*;


/**
 * Event Listener Implementation for auditing Keycloak events
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
                log.debug("Updating last login status for user: {} (client: {})", event.getUserId(), event.getClientId());
                // Use server time and set timezone for login event
                String defaultTimeZone = ConfigHelper.getConfigValue(ConfigConstants.DEFAULT_TIMEZONE);
                OffsetDateTime loginTime = OffsetDateTime.now(ZoneId.of(defaultTimeZone));
                String lastLoginAttribute = USER_EVENT_PREFIX.value() + "_" + LAST_LOGIN_INFIX.value();
                String loginTimeS = DateTimeFormatter.ISO_DATE_TIME.format(loginTime);
                user.setSingleAttribute(lastLoginAttribute, loginTimeS);
                user.setSingleAttribute(lastLoginAttribute + "_" + event.getClientId(), loginTimeS);
                AuditUserProfileRegistrar.registerForRealm(session, realm);
                AuditUserProfileRegistrar.registerClientLoginAttribute(session, realm, event.getClientId());
            }
        }
        if (EventType.CLIENT_LOGIN.equals(event.getType())) {
            RealmModel realm = this.model.getRealm(event.getRealmId());
            ClientModel client = this.session.clients().getClientByClientId(realm, event.getClientId());

            if (client != null) {
                log.debug("Updating last login status in client {} for user: {}", event.getClientId(), event.getUserId());
                // Use server time and set timezone for login event
                String defaultTimeZone = ConfigHelper.getConfigValue(ConfigConstants.DEFAULT_TIMEZONE);
                OffsetDateTime loginTime = OffsetDateTime.now(ZoneId.of(defaultTimeZone));
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
