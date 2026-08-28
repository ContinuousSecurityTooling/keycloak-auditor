package net.cst.keycloak.userprofile;

import lombok.extern.slf4j.Slf4j;
import net.cst.keycloak.audit.model.ConfigConstants;
import net.cst.keycloak.utils.ConfigHelper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPGroup;
import org.keycloak.userprofile.UserProfileConstants;
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
    static final String ADMIN_ROLE = UserProfileConstants.ROLE_ADMIN;
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
        if (config.getAttributes() != null) {
            UPAttribute existing = config.getAttributes().stream()
                    .filter(a -> name.equals(a.getName()))
                    .findFirst()
                    .orElse(null);
            if (existing != null) {
                return reconcileEditPermission(existing);
            }
        }

        UPAttribute attribute = new UPAttribute();
        attribute.setName(name);
        attribute.setDisplayName(displayName);
        attribute.setGroup(AUDIT_GROUP_NAME);

        UPAttributePermissions permissions = new UPAttributePermissions();
        permissions.setView(new HashSet<>(Set.of(ADMIN_ROLE)));
        permissions.setEdit(desiredEditPermission());
        attribute.setPermissions(permissions);

        if (config.getAttributes() == null) {
            config.setAttributes(new java.util.ArrayList<>());
        }
        config.getAttributes().add(attribute);
        return true;
    }

    /**
     * Aligns the edit permission of an already-registered attribute with the current
     * {@code KC_AUD_ALLOW_ADMIN_EDIT} setting. Returns {@code true} if the config was changed.
     */
    private static boolean reconcileEditPermission(UPAttribute attribute) {
        UPAttributePermissions permissions = attribute.getPermissions();
        if (permissions == null) {
            permissions = new UPAttributePermissions();
            permissions.setView(new HashSet<>(Set.of(ADMIN_ROLE)));
            attribute.setPermissions(permissions);
        }

        Set<String> desired = desiredEditPermission();
        Set<String> current = permissions.getEdit() == null ? new HashSet<>() : permissions.getEdit();
        if (current.equals(desired)) {
            return false;
        }
        permissions.setEdit(desired);
        return true;
    }

    /**
     * Empty by default (attribute stays read-only). When {@code KC_AUD_ALLOW_ADMIN_EDIT=true},
     * admins are granted edit rights so admin-side user updates that carry these attributes
     * are not rejected with {@code error-user-attribute-read-only}.
     */
    private static Set<String> desiredEditPermission() {
        if (ConfigHelper.getConfigToggle(ConfigConstants.ALLOW_ADMIN_EDIT)) {
            return new HashSet<>(Set.of(ADMIN_ROLE));
        }
        return new HashSet<>();
    }
}
