package net.cst.keycloak.utils;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author : mreinhardt
 * @created : 13.10.23
 **/
public class RuntimeHelper {
    private static final String MAVEN_PACKAGE = "net.continuous-security-tools";
    private static final String MAVEN_ARTIFACT = "spi";

    public synchronized static String getVersion() {
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
