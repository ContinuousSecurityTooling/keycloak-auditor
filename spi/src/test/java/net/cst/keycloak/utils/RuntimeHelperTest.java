package net.cst.keycloak.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : mreinhardt
 * @created : 14.10.23
 **/
class RuntimeHelperTest {

    @Test
    void shouldShowVersionInfo() {
        String version = RuntimeHelper.getVersion();
        assertNotNull(version);
    }
}
