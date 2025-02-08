package net.cst.keycloak.utils;

import net.cst.keycloak.audit.model.ConfigConstants;

public class ConfigHelper {

    private ConfigHelper() {
        // Utility class
    }

    public static boolean getConfigToggle(ConfigConstants config) {
        String value = getConfigValue(config);
        return Boolean.parseBoolean(value);
    }

    public static String getConfigValue(ConfigConstants config) {
        String value = getEnvValue(config.value());
        if (value == null) {
            value = config.getDefaultValue();
        }
        return value;
    }

    public static String getEnvValue(String value) {
        return System.getenv(value);
    }
}
