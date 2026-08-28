package net.cst.keycloak.audit.model;

import net.cst.keycloak.utils.ConfigHelper;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : mreinhardt
 **/
class ConfigConstantsTest {

    @Test
    void testDefaultValueForExternalAccess() {
        boolean externalAccess = ConfigHelper.getConfigToggle(ConfigConstants.DISABLE_EXTERNAL_ACCESS);
        assertFalse(externalAccess);
    }

    @Test
    void testDefaultValueForRoleCheck() {
        boolean roleCheck = ConfigHelper.getConfigToggle(ConfigConstants.DISABLE_ROLE_CHECK);
        assertFalse(roleCheck);
    }

    @Test
    void testDefaultValueForRolename() {
        String roleName = ConfigHelper.getConfigValue(ConfigConstants.DEFAULT_ROLE);
        assertEquals("admin", roleName);
    }

    @Test
    void testDefaultValueForAllowAdminEdit() {
        assertFalse(ConfigHelper.getConfigToggle(ConfigConstants.ALLOW_ADMIN_EDIT));
        assertEquals("KC_AUD_ALLOW_ADMIN_EDIT", ConfigConstants.ALLOW_ADMIN_EDIT.value());
    }

    @Test
    @SetEnvironmentVariable(key = "KC_AUD_ALLOW_ADMIN_EDIT", value = "true")
    void testAllowAdminEditCanBeEnabled() {
        assertTrue(ConfigHelper.getConfigToggle(ConfigConstants.ALLOW_ADMIN_EDIT));
    }
}
