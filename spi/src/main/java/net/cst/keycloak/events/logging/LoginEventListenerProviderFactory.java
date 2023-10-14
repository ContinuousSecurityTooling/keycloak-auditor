package net.cst.keycloak.events.logging;

import lombok.extern.slf4j.Slf4j;
import net.cst.keycloak.utils.RuntimeHelper;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Event Listener Factory
 *
 * @author : mreinhardt
 * @created : 22.06.23
 **/
@Slf4j
public class LoginEventListenerProviderFactory implements EventListenerProviderFactory {

    @Override
    public LoginEventListenerProvider create(KeycloakSession keycloakSession) {
        return new LoginEventListenerProvider(keycloakSession);
    }

    @Override
    public void init(Config.Scope scope) {
        log.info("Initializing Keycloak Auditor Listener (Version {}).",
                RuntimeHelper.getVersion());
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        //
    }

    @Override
    public void close() {
        //
    }

    @Override
    public String getId() {
        return "kc-auditor";
    }

}
