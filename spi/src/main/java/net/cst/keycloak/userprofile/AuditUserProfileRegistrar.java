package net.cst.keycloak.userprofile;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPGroup;
import org.keycloak.userprofile.UserProfileProvider;

import java.util.HashSet;
import java.util.Set;

import static net.cst.keycloak.audit.model.Constants.LAST_LOGIN_INFIX;
import static net.cst.keycloak.audit.model.Constants.USER_EVENT_PREFIX;

/**
 * Registers audit-related attributes in the realm's Declarative User Profile configuration
 * so that last-login timestamps are displayed with proper labels in the admin console.
 */
@Slf4j
public class AuditUserProfileRegistrar {

    static final String AUDIT_GROUP_NAME = "audit";
    static final String GLOBAL_ATTR_NAME = USER_EVENT_PREFIX.value() + "_" + LAST_LOGIN_INFIX.value();

    private AuditUserProfileRegistrar() {
    }

    /**
     * Ensures the global last-login attribute and the "Audit Information" group exist
     * in the user profile configuration for the given realm.
     */
    public static void registerForRealm(KeycloakSession session, RealmModel realm) {
        session.getContext().setRealm(realm);
        UserProfileProvider upp = session.getProvider(UserProfileProvider.class);
        UPConfig config = upp.getConfiguration();

        boolean modified = ensureAuditGroup(config);
        modified |= ensureAttribute(config, GLOBAL_ATTR_NAME, "Last Login");

        if (modified) {
            log.info("Registered audit user profile attributes for realm '{}'", realm.getName());
            upp.setConfiguration(config);
        }
    }

    /**
     * Ensures a per-client last-login attribute exists in the user profile configuration
     * for the given realm. Meant to be called from within an active event handler
     * (session context realm is already set).
     */
    public static void registerClientLoginAttribute(KeycloakSession session, RealmModel realm, String clientId) {
        session.getContext().setRealm(realm);
        UserProfileProvider upp = session.getProvider(UserProfileProvider.class);
        UPConfig config = upp.getConfiguration();

        String attrName = GLOBAL_ATTR_NAME + "_" + clientId;
        boolean modified = ensureAuditGroup(config);
        modified |= ensureAttribute(config, attrName, "Last Login (" + clientId + ")");

        if (modified) {
            log.info("Registered audit user profile attribute '{}' for realm '{}'", attrName, realm.getName());
            upp.setConfiguration(config);
        }
    }

    private static boolean ensureAuditGroup(UPConfig config) {
        if (config.getGroups() != null
                && config.getGroups().stream().anyMatch(g -> AUDIT_GROUP_NAME.equals(g.getName()))) {
            return false;
        }

        UPGroup group = new UPGroup();
        group.setName(AUDIT_GROUP_NAME);
        group.setDisplayHeader("Audit Information");
        config.addGroup(group);
        return true;
    }

    private static boolean ensureAttribute(UPConfig config, String name, String displayName) {
        if (config.getAttributes() != null
                && config.getAttributes().stream().anyMatch(a -> name.equals(a.getName()))) {
            return false;
        }

        UPAttribute attribute = new UPAttribute();
        attribute.setName(name);
        attribute.setDisplayName(displayName);
        attribute.setGroup(AUDIT_GROUP_NAME);

        UPAttributePermissions permissions = new UPAttributePermissions();
        permissions.setView(new HashSet<>(Set.of("admin")));
        permissions.setEdit(new HashSet<>());
        attribute.setPermissions(permissions);

        if (config.getAttributes() == null) {
            config.setAttributes(new java.util.ArrayList<>());
        }
        config.getAttributes().add(attribute);
        return true;
    }
}
