package net.cst.keycloak.userprofile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.userprofile.UserProfileProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditUserProfileRegistrarTest {

    private KeycloakSession session;
    private RealmModel realm;
    private UserProfileProvider upp;

    @BeforeEach
    void setup() {
        session = mock(KeycloakSession.class);
        realm = mock(RealmModel.class);
        KeycloakContext ctx = mock(KeycloakContext.class);
        upp = mock(UserProfileProvider.class);

        when(session.getContext()).thenReturn(ctx);
        when(session.getProvider(UserProfileProvider.class)).thenReturn(upp);
        when(realm.getName()).thenReturn("test-realm");
    }

    @Test
    void registerForRealmShouldAddAuditGroupAndGlobalAttribute() {
        UPConfig config = new UPConfig();
        when(upp.getConfiguration()).thenReturn(config);

        AuditUserProfileRegistrar.registerForRealm(session, realm);

        verify(upp).setConfiguration(config);
        assertNotNull(config.getGroups(), "Audit group list must be non-null");
        assertTrue(config.getGroups().stream()
                        .anyMatch(g -> AuditUserProfileRegistrar.AUDIT_GROUP_NAME.equals(g.getName())),
                "Audit group missing");
        assertNotNull(config.getAttributes(), "Attribute list must be non-null");
        assertTrue(config.getAttributes().stream()
                        .anyMatch(a -> AuditUserProfileRegistrar.GLOBAL_ATTR_NAME.equals(a.getName())),
                "Global last-login attribute missing");
    }

    @Test
    void registerForRealmShouldBeIdempotent() {
        UPConfig config = new UPConfig();
        when(upp.getConfiguration()).thenReturn(config);

        AuditUserProfileRegistrar.registerForRealm(session, realm);
        AuditUserProfileRegistrar.registerForRealm(session, realm);

        assertEquals(1, config.getGroups().size(), "Audit group must appear exactly once");
        assertEquals(1, config.getAttributes().size(), "Global attribute must appear exactly once");
        verify(upp, times(1)).setConfiguration(config);
    }

    @Test
    void registerClientLoginAttributeShouldAddPerClientAttribute() {
        UPConfig config = new UPConfig();
        when(upp.getConfiguration()).thenReturn(config);

        AuditUserProfileRegistrar.registerClientLoginAttribute(session, realm, "my-client");

        verify(upp).setConfiguration(config);
        String expected = AuditUserProfileRegistrar.GLOBAL_ATTR_NAME + "_my-client";
        assertTrue(config.getAttributes().stream().anyMatch(a -> expected.equals(a.getName())),
                "Per-client attribute missing");
    }

    @Test
    void registerClientLoginAttributeShouldBeIdempotent() {
        UPConfig config = new UPConfig();
        when(upp.getConfiguration()).thenReturn(config);

        AuditUserProfileRegistrar.registerClientLoginAttribute(session, realm, "my-client");
        AuditUserProfileRegistrar.registerClientLoginAttribute(session, realm, "my-client");

        String expected = AuditUserProfileRegistrar.GLOBAL_ATTR_NAME + "_my-client";
        long count = config.getAttributes().stream()
                .filter(a -> expected.equals(a.getName()))
                .count();
        assertEquals(1, count, "Per-client attribute must appear exactly once");
        verify(upp, times(1)).setConfiguration(config);
    }

    @Test
    void globalAttributeShouldBeAdminViewableAndNotEditable() {
        UPConfig config = new UPConfig();
        when(upp.getConfiguration()).thenReturn(config);

        AuditUserProfileRegistrar.registerForRealm(session, realm);

        UPAttribute attr = config.getAttributes().stream()
                .filter(a -> AuditUserProfileRegistrar.GLOBAL_ATTR_NAME.equals(a.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(attr, "Global attribute not found");
        assertNotNull(attr.getPermissions(), "Permissions must be set");
        assertTrue(attr.getPermissions().getView().contains("admin"), "Admin must have view permission");
        assertTrue(attr.getPermissions().getEdit().isEmpty(), "No one should be able to edit the attribute");
    }

    @Test
    void globalAttributeShouldBelongToAuditGroup() {
        UPConfig config = new UPConfig();
        when(upp.getConfiguration()).thenReturn(config);

        AuditUserProfileRegistrar.registerForRealm(session, realm);

        UPAttribute attr = config.getAttributes().stream()
                .filter(a -> AuditUserProfileRegistrar.GLOBAL_ATTR_NAME.equals(a.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(attr);
        assertEquals(AuditUserProfileRegistrar.AUDIT_GROUP_NAME, attr.getGroup());
    }

    @Test
    void differentClientsProduceDistinctAttributes() {
        UPConfig config = new UPConfig();
        when(upp.getConfiguration()).thenReturn(config);

        AuditUserProfileRegistrar.registerClientLoginAttribute(session, realm, "client-a");
        AuditUserProfileRegistrar.registerClientLoginAttribute(session, realm, "client-b");

        long count = config.getAttributes().stream()
                .filter(a -> a.getName().startsWith(AuditUserProfileRegistrar.GLOBAL_ATTR_NAME + "_"))
                .count();
        assertEquals(2, count, "Expected two distinct per-client attributes");
    }
}
