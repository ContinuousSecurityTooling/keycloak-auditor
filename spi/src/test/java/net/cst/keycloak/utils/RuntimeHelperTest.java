package net.cst.keycloak.utils;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuntimeHelperTest {

    @Test
    void shouldShowVersionInfo() {
        String version = RuntimeHelper.getVersion();
        assertNotNull(version);
    }

    @Test
    void getVersionShouldReadFromMavenMetadataOnClasspathWhenPresent() {
        // spi/src/test/resources/META-INF/maven/.../pom.properties supplies this for the test
        // classpath, exercising the primary (real jar) lookup path end-to-end.
        assertEquals("9.9.9-test", RuntimeHelper.getVersion());
    }

    // -----------------------------------------------------------------------
    // parseVersion
    // -----------------------------------------------------------------------

    @Test
    void parseVersionShouldReturnNullWhenStreamIsNull() {
        assertNull(RuntimeHelper.parseVersion(null));
    }

    @Test
    void parseVersionShouldReturnTrimmedVersionWhenPresent() {
        InputStream is = propertiesStream("version=  1.2.3  \n");
        assertEquals("1.2.3", RuntimeHelper.parseVersion(is));
    }

    @Test
    void parseVersionShouldReturnNullWhenVersionPropertyMissing() {
        InputStream is = propertiesStream("artifactId=keycloak-auditor-spi\n");
        assertNull(RuntimeHelper.parseVersion(is));
    }

    @Test
    void parseVersionShouldReturnNullWhenVersionPropertyBlank() {
        InputStream is = propertiesStream("version=   \n");
        assertNull(RuntimeHelper.parseVersion(is));
    }

    @Test
    void parseVersionShouldReturnNullWhenStreamCannotBeRead() {
        InputStream broken = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }
        };
        assertNull(RuntimeHelper.parseVersion(broken));
    }

    private static InputStream propertiesStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    // -----------------------------------------------------------------------
    // fallbackVersion
    // -----------------------------------------------------------------------

    @Test
    void fallbackVersionShouldReturnUnknownWhenPackageIsNull() {
        assertEquals("unknown", RuntimeHelper.fallbackVersion(null));
    }

    @Test
    void fallbackVersionShouldReturnImplementationVersionWhenPresent() {
        Package pkg = mock(Package.class);
        when(pkg.getImplementationVersion()).thenReturn(" 4.5.6 ");
        assertEquals("4.5.6", RuntimeHelper.fallbackVersion(pkg));
    }

    @Test
    void fallbackVersionShouldFallBackToSpecificationVersionWhenImplementationMissing() {
        Package pkg = mock(Package.class);
        when(pkg.getImplementationVersion()).thenReturn(null);
        when(pkg.getSpecificationVersion()).thenReturn("7.8.9");
        assertEquals("7.8.9", RuntimeHelper.fallbackVersion(pkg));
    }

    @Test
    void fallbackVersionShouldReturnUnknownWhenNeitherVersionPresent() {
        Package pkg = mock(Package.class);
        when(pkg.getImplementationVersion()).thenReturn(null);
        when(pkg.getSpecificationVersion()).thenReturn(null);
        assertEquals("unknown", RuntimeHelper.fallbackVersion(pkg));
    }

    @Test
    void fallbackVersionShouldReturnUnknownWhenVersionIsBlank() {
        Package pkg = mock(Package.class);
        when(pkg.getImplementationVersion()).thenReturn("   ");
        when(pkg.getSpecificationVersion()).thenReturn(null);
        assertEquals("unknown", RuntimeHelper.fallbackVersion(pkg));
    }
}
