package net.cst.keycloak.events.logging;

import net.cst.keycloak.utils.EndpointTest;
import net.cst.keycloak.userprofile.AuditUserProfileRegistrar;
import org.junit.jupiter.api.Test;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginEventListenerProviderFactoryTest extends EndpointTest {

    @Test
    void shouldCreateProvider() {
        assertNotNull(new LoginEventListenerProviderFactory().create(session));
    }

    @Test
    void getIdShouldReturnKcAuditor() {
        assertEquals("kc-auditor", new LoginEventListenerProviderFactory().getId());
    }

    @Test
    void initShouldNotThrow() {
        assertDoesNotThrow(() -> new LoginEventListenerProviderFactory().init(mock(Config.Scope.class)));
    }

    @Test
    void closeShouldNotThrow() {
        assertDoesNotThrow(() -> new LoginEventListenerProviderFactory().close());
    }

    @Test
    void postInitShouldOnlyRegisterListenerAndNotTouchRealmsYet() {
        // Regression test for #881: postInit() must not access session.realms()/JPA directly,
        // since that races with AbstractJpaConnectionProviderFactory's EntityManagerFactory
        // initialization and crashes Keycloak at startup with a NullPointerException.
        KeycloakSessionFactory factory = mock(KeycloakSessionFactory.class);

        try (MockedStatic<KeycloakModelUtils> utils = mockStatic(KeycloakModelUtils.class)) {
            new LoginEventListenerProviderFactory().postInit(factory);

            verify(factory).register(any(ProviderEventListener.class));
            utils.verifyNoInteractions();
            verifyNoInteractions(session);
        }
    }

    @Test
    void postInitListenerShouldRegisterUserProfileForEachRealmOnPostMigrationEvent() {
        KeycloakSessionFactory factory = mock(KeycloakSessionFactory.class);
        RealmModel realm1 = mock(RealmModel.class);
        RealmModel realm2 = mock(RealmModel.class);
        when(realm1.getId()).thenReturn("id-1");
        when(realm2.getId()).thenReturn("id-2");
        RealmProvider realmProvider = mock(RealmProvider.class);
        when(realmProvider.getRealmsStream()).thenReturn(Stream.of(realm1, realm2));
        when(realmProvider.getRealm("id-1")).thenReturn(realm1);
        when(realmProvider.getRealm("id-2")).thenReturn(realm2);
        when(session.realms()).thenReturn(realmProvider);

        UserProfileProvider upp = mock(UserProfileProvider.class);
        when(session.getProvider(UserProfileProvider.class)).thenReturn(upp);
        when(upp.getConfiguration()).thenReturn(new UPConfig());

        try (MockedStatic<KeycloakModelUtils> utils = mockStatic(KeycloakModelUtils.class)) {
            utils.when(() -> KeycloakModelUtils.runJobInTransaction(eq(factory), any()))
                 .thenAnswer(inv -> {
                     org.keycloak.models.KeycloakSessionTask task = inv.getArgument(1);
                     task.run(session);
                     return null;
                 });
            try (MockedStatic<AuditUserProfileRegistrar> reg = mockStatic(AuditUserProfileRegistrar.class)) {
                new LoginEventListenerProviderFactory().postInit(factory);

                ArgumentCaptor<ProviderEventListener> captor = ArgumentCaptor.forClass(ProviderEventListener.class);
                verify(factory).register(captor.capture());
                ProviderEventListener listener = captor.getValue();

                PostMigrationEvent event = mock(PostMigrationEvent.class);
                listener.onEvent(event);

                reg.verify(() -> AuditUserProfileRegistrar.registerForRealm(session, realm1));
                reg.verify(() -> AuditUserProfileRegistrar.registerForRealm(session, realm2));
                verify(factory).unregister(listener);
            }
        }
    }

    @Test
    void postInitListenerShouldSkipNullRealms() {
        KeycloakSessionFactory factory = mock(KeycloakSessionFactory.class);
        RealmModel realm1 = mock(RealmModel.class);
        when(realm1.getId()).thenReturn("id-1");
        RealmProvider realmProvider = mock(RealmProvider.class);
        when(realmProvider.getRealmsStream()).thenReturn(Stream.of(realm1));
        when(realmProvider.getRealm("id-1")).thenReturn(null); // realm disappeared
        when(session.realms()).thenReturn(realmProvider);

        try (MockedStatic<KeycloakModelUtils> utils = mockStatic(KeycloakModelUtils.class)) {
            utils.when(() -> KeycloakModelUtils.runJobInTransaction(eq(factory), any()))
                 .thenAnswer(inv -> {
                     org.keycloak.models.KeycloakSessionTask task = inv.getArgument(1);
                     task.run(session);
                     return null;
                 });
            try (MockedStatic<AuditUserProfileRegistrar> reg = mockStatic(AuditUserProfileRegistrar.class)) {
                LoginEventListenerProviderFactory factoryUnderTest = new LoginEventListenerProviderFactory();
                assertDoesNotThrow(() -> factoryUnderTest.postInit(factory));

                ArgumentCaptor<ProviderEventListener> captor = ArgumentCaptor.forClass(ProviderEventListener.class);
                verify(factory).register(captor.capture());
                ProviderEventListener listener = captor.getValue();

                assertDoesNotThrow(() -> listener.onEvent(mock(PostMigrationEvent.class)));
                reg.verify(() -> AuditUserProfileRegistrar.registerForRealm(any(), any()), never());
            }
        }
    }

    @Test
    void postInitListenerShouldIgnoreUnrelatedProviderEvents() {
        KeycloakSessionFactory factory = mock(KeycloakSessionFactory.class);

        try (MockedStatic<KeycloakModelUtils> utils = mockStatic(KeycloakModelUtils.class)) {
            new LoginEventListenerProviderFactory().postInit(factory);

            ArgumentCaptor<ProviderEventListener> captor = ArgumentCaptor.forClass(ProviderEventListener.class);
            verify(factory).register(captor.capture());
            ProviderEventListener listener = captor.getValue();

            ProviderEvent unrelatedEvent = mock(ProviderEvent.class);
            listener.onEvent(unrelatedEvent);

            utils.verifyNoInteractions();
            verify(factory, never()).unregister(any());
        }
    }
}
