package net.cst.keycloak.audit.model;

import lombok.Getter;
import lombok.Setter;
import org.keycloak.representations.idm.ClientRepresentation;

/**
 * @author : mreinhardt
 **/
@Getter
@Setter
public class AuditedClientRepresentation extends ClientRepresentation {

    private String realm;

    private String lastLogin;
}
