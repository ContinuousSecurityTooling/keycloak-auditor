package net.cst.keycloak.audit.model;

import lombok.Getter;
import lombok.Setter;
import org.keycloak.representations.idm.ClientRepresentation;

/**
 * @author : mreinhardt
 * @created : 13.07.23
 **/
@Getter
@Setter
public class AuditedClientRepresentation extends ClientRepresentation {

    private String lastLogin;
}
