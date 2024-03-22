package net.cst.keycloak.utils;

import java.io.InputStream;
import java.util.Properties;

/**
 * @author : mreinhardt
 * @created : 13.10.23
 **/
public class RuntimeHelper {
    private static final String MAVEN_PACKAGE = "net.continuous-security-tools";
    private static final String MAVEN_ARTIFACT = "spi";

    private RuntimeHelper() {
    }

    public static synchronized String getVersion() {
        // Try to get version number from maven properties in jar's META-INF
        try (InputStream is = RuntimeHelper.class
                .getResourceAsStream("/META-INF/maven/" + MAVEN_PACKAGE + "/"
                        + MAVEN_ARTIFACT + "/pom.properties")) {
            if (is != null) {
                Properties p = new Properties();
                p.load(is);
                String version = p.getProperty("version", "").trim();
                if (!version.isEmpty()) {
                    return version;
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        // Fallback to using Java API to get version from MANIFEST.MF
        String version = null;
        Package pkg = RuntimeHelper.class.getPackage();
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
