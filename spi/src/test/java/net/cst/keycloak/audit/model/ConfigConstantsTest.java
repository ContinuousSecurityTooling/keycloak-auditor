package net.cst.keycloak.audit.model;

import net.cst.keycloak.utils.ConfigHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : mreinhardt
 * @created : 09.10.23
 **/
class ConfigConstantsTest {

    @Test
    void testDefaultValueForExternalAccess() {
        boolean externalAccess = ConfigHelper.getConfigToggle(ConfigConstants.DISABLE_EXTERNAL_ACCESS);
        assertTrue(externalAccess);
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
}
