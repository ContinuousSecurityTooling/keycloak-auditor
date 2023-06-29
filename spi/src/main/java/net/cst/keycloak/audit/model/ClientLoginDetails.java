package net.cst.keycloak.audit.model;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Date;

/**
 * @author : mreinhardt
 * @created : 29.06.23
 **/
@Getter
@Setter
public class ClientLoginDetails {
    OffsetDateTime kcLogin;
}
