package net.cst.keycloak.audit.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ConfigConstants {
    DISABLE_EXTERNAL_ACCESS("KC_AUD_DISABLE_EXTERNAL_ACCESS", "false"),
    DISABLE_ROLE_CHECK("KC_AUD_DISABLE_ROLE_CHECK", "false"),
    GLOBAL_MASTER_ACCESS("KC_AUD_GLOBAL_MASTER_ACCESS", "false"),
    DEFAULT_ROLE("KC_AUD_DEFAULT_ROLE", "admin"),
    DEFAULT_TIMEZONE("KC_AUD_DEFAULT_TIMEZONE", "UTC");

    @JsonValue
    private final String value;

    private final String defaultValue;

    public final String value() {
        return this.value;
    }

    public final String getDefaultValue() {
        return this.defaultValue;
    }


}
