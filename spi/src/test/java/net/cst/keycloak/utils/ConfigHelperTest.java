package net.cst.keycloak.utils;

import net.cst.keycloak.audit.model.ConfigConstants;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

class ConfigHelperTest {
    @Test
    @SetEnvironmentVariable(key = "KC_AUD_GLOBAL_MASTER_ACCESS", value = "false")
    void shouldReadConfigGlobalMasterAccessDisbled() {
        var toggleEnabled = ConfigHelper.getConfigToggle(ConfigConstants.GLOBAL_MASTER_ACCESS);
        assertFalse(toggleEnabled);
    }

    @Test
    @SetEnvironmentVariable(key = "KC_AUD_GLOBAL_MASTER_ACCESS", value = "true")
    void shouldReadConfigGlobalMasterAccessEnabled() {
        var toggleEnabled = ConfigHelper.getConfigToggle(ConfigConstants.GLOBAL_MASTER_ACCESS);
        assertTrue(toggleEnabled);
    }

    @Test
    @SetEnvironmentVariable(key = "KC_AUD_DISABLE_ROLE_CHECK", value = "false")
    void shouldReadConfigRoleCheckEnabled() {
        var globalAccessEnabled = ConfigHelper.getConfigToggle(ConfigConstants.DISABLE_ROLE_CHECK);
        assertFalse(globalAccessEnabled);
    }

    @Test
    @SetEnvironmentVariable(key = "KC_AUD_DISABLE_ROLE_CHECK", value = "true")
    void shouldReadConfigRoleCheckDisabled() {
        var globalAccessEnabled = ConfigHelper.getConfigToggle(ConfigConstants.DISABLE_ROLE_CHECK);
        assertTrue(globalAccessEnabled);
    }

    @Test
    void shouldReadDefaultRole() {
        var roleName = ConfigHelper.getConfigValue(ConfigConstants.DEFAULT_ROLE);
        assertEquals("admin", roleName);
    }

    @Test
    @SetEnvironmentVariable(key = "KC_AUD_DEFAULT_ROLE", value = "test")
    void shouldReadDefaultRoleOverridden() {
        var roleName = ConfigHelper.getConfigValue(ConfigConstants.DEFAULT_ROLE);
        assertEquals("test", roleName);
    }

}
