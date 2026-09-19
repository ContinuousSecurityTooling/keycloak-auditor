package net.cst.keycloak.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class RuntimeHelper {

    private static final String MAVEN_PACKAGE = "net.continuous-security-tools";
    private static final String MAVEN_ARTIFACT = "keycloak-auditor-spi";

    private RuntimeHelper() {
        // Utility class
    }

    public static synchronized String getVersion() {
        // Try to get version number from maven properties in jar's META-INF
        try (InputStream is = RuntimeHelper.class
                .getResourceAsStream(
                        "/META-INF/maven/" + MAVEN_PACKAGE + "/" + MAVEN_ARTIFACT + "/pom.properties")) {
            String version = parseVersion(is);
            if (version != null) {
                return version;
            }
        } catch (IOException e) {
            // Ignore
        }

        // Fallback to using Java API to get version from MANIFEST.MF
        return fallbackVersion(RuntimeHelper.class.getPackage());
    }

    /**
     * @return the trimmed {@code version} property from {@code is}, or {@code null} if it's
     * absent, blank, or the stream can't be read as a properties file.
     */
    static String parseVersion(InputStream is) {
        if (is == null) {
            return null;
        }
        try {
            Properties p = new Properties();
            p.load(is);
            String version = p.getProperty("version", "").trim();
            return version.isEmpty() ? null : version;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @return the trimmed implementation/specification version from {@code pkg}, or
     * {@code "unknown"} if neither is present.
     */
    static String fallbackVersion(Package pkg) {
        String version = null;
        if (pkg != null) {
            version = pkg.getImplementationVersion();
            if (version == null) {
                version = pkg.getSpecificationVersion();
            }
        }
        version = version == null ? "" : version.trim();
        return version.isEmpty() ? "unknown" : version;
    }
}
