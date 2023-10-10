package net.cst.keycloak.audit.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author : mreinhardt
 * @created : 22.06.23
 **/
public enum ConfigConstants {
    DISABLE_EXTERNAL_ACCESS("KC_AUD_DISABLE_EXTERNAL_ACCESS", "true"),
    DISABLE_ROLE_CHECK("KC_AUD_DISABLE_ROLE_CHECK", "false"),
    DEFAULT_ROLE("KC_AUD_DEFAULT_ROLE", "admin");

    @JsonValue
    private final String value;
    
    private final String defaultValue;

    private ConfigConstants(String value, String defaultValue) {
        this.value = value;
        this.defaultValue = defaultValue;
    }

    public final String value() {
        return this.value;
    }
    public final String getDefaultValue() {
        return this.defaultValue;
    }
}
