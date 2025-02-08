package net.cst.keycloak.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeHelperTest {

    @Test
    void shouldShowVersionInfo() {
        String version = RuntimeHelper.getVersion();
        assertNotNull(version);
    }
}
