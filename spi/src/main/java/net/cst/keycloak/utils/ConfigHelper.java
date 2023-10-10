package net.cst.keycloak.utils;

import net.cst.keycloak.audit.model.ConfigConstants;

/**
 * @author : mreinhardt
 * @created : 09.10.23
 **/
public class ConfigHelper {

    public static boolean getConfigToggle(ConfigConstants config) {
        String value = System.getenv(config.value());
        if (value == null) {
            value = config.getDefaultValue();
        }
        return Boolean.parseBoolean(value);
    }

    public static String getConfigValue(ConfigConstants config) {
        String value = System.getenv(config.value());
        if (value == null) {
            value = config.getDefaultValue();
        }
        return value;
    }
}
