package net.cst.keycloak.userprofile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.userprofile.UserProfileConstants;
import org.keycloak.userprofile.UserProfileProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditUserProfileRegistrarTest {

    private static final String ADMIN = UserProfileConstants.ROLE_ADMIN;
    private static final String GLOBAL_ATTR = AuditUserProfileRegistrar.GLOBAL_ATTR_NAME;
    private static final String ALLOW_ADMIN_EDIT = "KC_AUD_ALLOW_ADMIN_EDIT";

    private KeycloakSession session;
    private KeycloakContext ctx;
    private RealmModel realm;
    private UserProfileProvider upp;

    @BeforeEach
    void setup() {
        session = mock(KeycloakSession.class);
        realm = mock(RealmModel.class);
        ctx = mock(KeycloakContext.class);
        upp = mock(UserProfileProvider.class);

        when(session.getContext()).thenReturn(ctx);
        when(session.getProvider(UserProfileProvider.class)).thenReturn(upp);
        when(realm.getName()).thenReturn("test-realm");
    }

    private UPConfig freshConfig() {
        UPConfig config = new UPConfig();
        when(upp.getConfiguration()).thenReturn(config);
        return config;
    }

    private static UPAttribute attribute(UPConfig config, String name) {
        assertNotNull(config.getAttributes(), "Attribute list must be non-null");
        return config.getAttributes().stream()
                .filter(a -> name.equals(a.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("attribute not found: " + name));
    }

    private UPAttribute existingGlobalAttribute(UPConfig config, Set<String> edit) {
        UPAttribute attr = new UPAttribute();
        attr.setName(GLOBAL_ATTR);
        attr.setGroup(AuditUserProfileRegistrar.AUDIT_GROUP_NAME);
        UPAttributePermissions permissions = new UPAttributePermissions();
        permissions.setView(new HashSet<>(Set.of(ADMIN)));
        permissions.setEdit(edit == null ? null : new HashSet<>(edit));
        attr.setPermissions(permissions);
        config.setAttributes(new ArrayList<>(List.of(attr)));
        return attr;
    }

    /**
     * Mirrors the write-permission predicate Keycloak builds in
     * {@code DeclarativeUserProfileProvider#createAttributeMetadata} (26.5.4, lines ~308-317):
     * an attribute update from the admin / User API context is rejected with
     * {@code error-user-attribute-read-only} (see {@code ImmutableAttributeValidator}) unless the
     * attribute's {@code edit} permissions contain the {@code admin} pseudo-role.
     */
    private static boolean adminWriteWouldBeRejected(UPAttribute attr) {
        Set<String> edit = attr.getPermissions() == null ? null : attr.getPermissions().getEdit();
        return edit == null || !edit.contains(UserProfileConstants.ROLE_ADMIN);
    }

    // --- registration & idempotency -------------------------------------------------

    @Test
    void registerForRealmAddsAuditGroupAndGlobalAttribute() {
        UPConfig config = freshConfig();

        AuditUserProfileRegistrar.registerForRealm(session, realm);

        verify(upp).setConfiguration(config);
        assertNotNull(config.getGroups(), "Audit group list must be non-null");
        assertTrue(config.getGroups().stream()
                        .anyMatch(g -> AuditUserProfileRegistrar.AUDIT_GROUP_NAME.equals(g.getName())),
                "Audit group missing");
        assertTrue(config.getAttributes().stream().anyMatch(a -> GLOBAL_ATTR.equals(a.getName())),
                "Global last-login attribute missing");
    }

    @Test
    void globalAttributeBelongsToAuditGroup() {
        UPConfig config = freshConfig();

        AuditUserProfileRegistrar.registerForRealm(session, realm);

        assertEquals(AuditUserProfileRegistrar.AUDIT_GROUP_NAME, attribute(config, GLOBAL_ATTR).getGroup());
    }

    @Test
    void auditGroupHasReadableHeader() {
        UPConfig config = freshConfig();

        AuditUserProfileRegistrar.registerForRealm(session, realm);

        assertTrue(config.getGroups().stream().anyMatch(g -> "Audit Information".equals(g.getDisplayHeader())),
                "Audit group header missing");
    }

    @Test
    void realmIsSetOnContextBeforeTouchingUserProfileProvider() {
        freshConfig();

        AuditUserProfileRegistrar.registerForRealm(session, realm);

        verify(ctx).setRealm(realm);
    }

    @Test
    void registerForRealmIsIdempotent() {
        UPConfig config = freshConfig();

        AuditUserProfileRegistrar.registerForRealm(session, realm);
        AuditUserProfileRegistrar.registerForRealm(session, realm);

        assertEquals(1, config.getGroups().size(), "Audit group must appear exactly once");
        assertEquals(1, config.getAttributes().size(), "Global attribute must appear exactly once");
        verify(upp, times(1)).setConfiguration(config);
    }

    @Test
    void registerClientLoginAttributeAddsPerClientAttribute() {
        UPConfig config = freshConfig();

        AuditUserProfileRegistrar.registerClientLoginAttribute(session, realm, "my-client");

        verify(upp).setConfiguration(config);
        UPAttribute attr = attribute(config, GLOBAL_ATTR + "_my-client");
        assertEquals(AuditUserProfileRegistrar.AUDIT_GROUP_NAME, attr.getGroup());
        assertEquals("Last Login (my-client)", attr.getDisplayName());
    }

    @Test
    void registerClientLoginAttributeIsIdempotent() {
        UPConfig config = freshConfig();

        AuditUserProfileRegistrar.registerClientLoginAttribute(session, realm, "my-client");
        AuditUserProfileRegistrar.registerClientLoginAttribute(session, realm, "my-client");

        long count = config.getAttributes().stream()
                .filter(a -> (GLOBAL_ATTR + "_my-client").equals(a.getName()))
                .count();
        assertEquals(1, count, "Per-client attribute must appear exactly once");
        verify(upp, times(1)).setConfiguration(config);
    }

    @Test
    void registerClientLoginAttributeReusesExistingAuditGroup() {
        UPConfig config = freshConfig();

        AuditUserProfileRegistrar.registerForRealm(session, realm);
        AuditUserProfileRegistrar.registerClientLoginAttribute(session, realm, "my-client");

        assertEquals(1, config.getGroups().size(), "Audit group must not be duplicated");
    }

    @Test
    void differentClientsProduceDistinctAttributes() {
        UPConfig config = freshConfig();

        AuditUserProfileRegistrar.registerClientLoginAttribute(session, realm, "client-a");
        AuditUserProfileRegistrar.registerClientLoginAttribute(session, realm, "client-b");

        long count = config.getAttributes().stream()
                .filter(a -> a.getName().startsWith(GLOBAL_ATTR + "_"))
                .count();
        assertEquals(2, count, "Expected two distinct per-client attributes");
    }

    // --- default read-only behaviour ----------------------------------------------

    @Test
    void globalAttributeIsAdminViewableAndNotEditableByDefault() {
        UPConfig config = freshConfig();

        AuditUserProfileRegistrar.registerForRealm(session, realm);

        UPAttributePermissions permissions = attribute(config, GLOBAL_ATTR).getPermissions();
        assertNotNull(permissions, "Permissions must be set");
        assertEquals(Set.of(ADMIN), permissions.getView(), "Only admin may view");
        assertTrue(permissions.getEdit().isEmpty(), "Nobody may edit by default");
    }

    @Test
    void perClientAttributeIsAdminViewableAndNotEditableByDefault() {
        UPConfig config = freshConfig();

        AuditUserProfileRegistrar.registerClientLoginAttribute(session, realm, "jira");

        UPAttributePermissions permissions = attribute(config, GLOBAL_ATTR + "_jira").getPermissions();
        assertEquals(Set.of(ADMIN), permissions.getView());
        assertTrue(permissions.getEdit().isEmpty());
    }

    @Test
    void adminUpdateCarryingAuditAttributesWouldBeRejectedByDefault() {
        UPConfig config = freshConfig();

        AuditUserProfileRegistrar.registerForRealm(session, realm);
        AuditUserProfileRegistrar.registerClientLoginAttribute(session, realm, "jira");

        assertTrue(adminWriteWouldBeRejected(attribute(config, GLOBAL_ATTR)),
                "read-only global attribute must still reject admin writes by default");
        assertTrue(adminWriteWouldBeRejected(attribute(config, GLOBAL_ATTR + "_jira")),
                "read-only per-client attribute must still reject admin writes by default");
    }

    // --- KC_AUD_ALLOW_ADMIN_EDIT=true --------------------------------------------

    @Test
    @SetEnvironmentVariable(key = ALLOW_ADMIN_EDIT, value = "true")
    void globalAttributeBecomesAdminEditableWhenToggleEnabled() {
        UPConfig config = freshConfig();

        AuditUserProfileRegistrar.registerForRealm(session, realm);

        UPAttributePermissions permissions = attribute(config, GLOBAL_ATTR).getPermissions();
        assertEquals(Set.of(ADMIN), permissions.getEdit(), "admin must be granted edit rights");
        assertEquals(Set.of(ADMIN), permissions.getView(), "view stays admin-only");
    }

    @Test
    @SetEnvironmentVariable(key = ALLOW_ADMIN_EDIT, value = "true")
    void perClientAttributeBecomesAdminEditableWhenToggleEnabled() {
        UPConfig config = freshConfig();

        AuditUserProfileRegistrar.registerClientLoginAttribute(session, realm, "jira");

        assertEquals(Set.of(ADMIN), attribute(config, GLOBAL_ATTR + "_jira").getPermissions().getEdit());
    }

    @Test
    @SetEnvironmentVariable(key = ALLOW_ADMIN_EDIT, value = "true")
    void adminUpdateCarryingAuditAttributesIsNotRejectedWhenToggleEnabled() {
        UPConfig config = freshConfig();

        AuditUserProfileRegistrar.registerForRealm(session, realm);
        AuditUserProfileRegistrar.registerClientLoginAttribute(session, realm, "jira");

        // Reproduces the fields from the original failure:
        //   aud_usr_last-login and aud_usr_last-login_jira -> error-user-attribute-read-only
        assertFalse(adminWriteWouldBeRejected(attribute(config, GLOBAL_ATTR)),
                "aud_usr_last-login must be admin-writable when KC_AUD_ALLOW_ADMIN_EDIT=true");
        assertFalse(adminWriteWouldBeRejected(attribute(config, GLOBAL_ATTR + "_jira")),
                "aud_usr_last-login_jira must be admin-writable when KC_AUD_ALLOW_ADMIN_EDIT=true");
    }

    @Test
    @SetEnvironmentVariable(key = ALLOW_ADMIN_EDIT, value = "true")
    void reRegistrationWithMatchingPermissionsDoesNotRewriteConfig() {
        UPConfig config = freshConfig();

        AuditUserProfileRegistrar.registerForRealm(session, realm);
        AuditUserProfileRegistrar.registerForRealm(session, realm);

        verify(upp, times(1)).setConfiguration(config);
    }

    @Test
    @SetEnvironmentVariable(key = ALLOW_ADMIN_EDIT, value = "TRUE")
    void toggleParsingIsCaseInsensitive() {
        UPConfig config = freshConfig();

        AuditUserProfileRegistrar.registerForRealm(session, realm);

        assertEquals(Set.of(ADMIN), attribute(config, GLOBAL_ATTR).getPermissions().getEdit());
    }

    @Test
    @SetEnvironmentVariable(key = ALLOW_ADMIN_EDIT, value = "yes")
    void nonBooleanToggleValueKeepsAttributeReadOnly() {
        UPConfig config = freshConfig();

        AuditUserProfileRegistrar.registerForRealm(session, realm);

        assertTrue(attribute(config, GLOBAL_ATTR).getPermissions().getEdit().isEmpty(),
                "only an explicit 'true' should unlock editing");
    }

    // --- permission reconciliation on re-registration ---------------------------

    @Test
    @SetEnvironmentVariable(key = ALLOW_ADMIN_EDIT, value = "true")
    void existingReadOnlyAttributeIsUpgradedWhenToggleEnabled() {
        UPConfig config = freshConfig();
        UPAttribute stale = existingGlobalAttribute(config, Set.of());

        AuditUserProfileRegistrar.registerForRealm(session, realm);

        verify(upp).setConfiguration(config);
        assertEquals(Set.of(ADMIN), stale.getPermissions().getEdit());
    }

    @Test
    void existingEditableAttributeIsDowngradedWhenToggleDisabled() {
        UPConfig config = freshConfig();
        UPAttribute stale = existingGlobalAttribute(config, Set.of(ADMIN));

        AuditUserProfileRegistrar.registerForRealm(session, realm);

        verify(upp).setConfiguration(config);
        assertTrue(stale.getPermissions().getEdit().isEmpty(),
                "edit rights must be revoked when KC_AUD_ALLOW_ADMIN_EDIT is not set");
    }

    @Test
    void unchangedReadOnlyAttributeIsRewrittenOnlyForTheMissingGroup() {
        UPConfig config = freshConfig();
        existingGlobalAttribute(config, Set.of());

        AuditUserProfileRegistrar.registerForRealm(session, realm);
        AuditUserProfileRegistrar.registerForRealm(session, realm);

        // first call still has to create the audit group; the attribute itself never changes
        verify(upp, times(1)).setConfiguration(config);
    }

    @Test
    @SetEnvironmentVariable(key = ALLOW_ADMIN_EDIT, value = "true")
    void existingAttributeWithNullPermissionsGetsPopulated() {
        UPConfig config = freshConfig();
        UPAttribute attr = new UPAttribute();
        attr.setName(GLOBAL_ATTR);
        attr.setPermissions(null);
        config.setAttributes(new ArrayList<>(List.of(attr)));

        AuditUserProfileRegistrar.registerForRealm(session, realm);

        assertNotNull(attr.getPermissions(), "permissions must be created");
        assertEquals(Set.of(ADMIN), attr.getPermissions().getView());
        assertEquals(Set.of(ADMIN), attr.getPermissions().getEdit());
    }

    @Test
    @SetEnvironmentVariable(key = ALLOW_ADMIN_EDIT, value = "true")
    void existingAttributeWithNullEditSetIsUpgraded() {
        UPConfig config = freshConfig();
        UPAttribute stale = existingGlobalAttribute(config, null);

        AuditUserProfileRegistrar.registerForRealm(session, realm);

        assertEquals(Set.of(ADMIN), stale.getPermissions().getEdit());
    }
}
