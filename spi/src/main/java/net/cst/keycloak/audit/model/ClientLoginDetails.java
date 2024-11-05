package net.cst.keycloak.audit.model;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * @author : mreinhardt
 **/
@Getter
@Setter
public class ClientLoginDetails {
    OffsetDateTime kcLogin;
}
