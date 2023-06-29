package net.cst.keycloak.audit.model;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author : mreinhardt
 * @created : 29.06.23
 **/
@Getter
@Setter
public class UserLoginDetails {
    OffsetDateTime kcLogin;
    Map<String, OffsetDateTime> clientLogins = new HashMap<>();
}
