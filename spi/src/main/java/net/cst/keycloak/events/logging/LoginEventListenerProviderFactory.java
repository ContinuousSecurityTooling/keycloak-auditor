package net.cst.keycloak.events.logging;

import lombok.extern.slf4j.Slf4j;
import net.cst.keycloak.userprofile.AuditUserProfileRegistrar;
import net.cst.keycloak.utils.RuntimeHelper;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;

import java.util.List;

/**
 * Event Listener Factory
 *
 *
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
    public void postInit(KeycloakSessionFactory factory) {
        // Realm/JPA access must wait until after model migration, otherwise it races with
        // AbstractJpaConnectionProviderFactory's EntityManagerFactory initialization and
        // crashes Keycloak at startup (NPE: "emf" is null). See issue #881.
        factory.register(new ProviderEventListener() {
            @Override
            public void onEvent(ProviderEvent event) {
                if (event instanceof PostMigrationEvent) {
                    factory.unregister(this);
                    registerUserProfileForAllRealms(factory);
                }
            }
        });
    }

    private void registerUserProfileForAllRealms(KeycloakSessionFactory factory) {
        KeycloakModelUtils.runJobInTransaction(factory, session -> {
            List<String> realmIds = session.realms().getRealmsStream()
                    .map(RealmModel::getId)
                    .toList();
            for (String realmId : realmIds) {
                RealmModel realm = session.realms().getRealm(realmId);
                if (realm != null) {
                    AuditUserProfileRegistrar.registerForRealm(session, realm);
                }
            }
        });
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
