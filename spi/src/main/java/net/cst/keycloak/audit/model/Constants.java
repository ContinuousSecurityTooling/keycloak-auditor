package net.cst.keycloak.audit.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author : mreinhardt
 *
 **/
public enum Constants {
    USER_EVENT_PREFIX("aud_usr"),
    CLIENT_EVENT_PREFIX("aud_cls"),
    ADMIN_EVENT_PREFIX("aud_adm"),
    LAST_LOGIN_INFIX("last-login");

    @JsonValue
    private final String value;

    private Constants(String value) {
        this.value = value;
    }

    public final String value() {
        return this.value;
    }
}
