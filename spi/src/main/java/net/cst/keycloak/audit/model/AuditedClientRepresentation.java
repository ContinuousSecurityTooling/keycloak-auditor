package net.cst.keycloak.audit.model;

import lombok.Getter;
import lombok.Setter;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.HashMap;
import java.util.Map;

/**
 * @author : mreinhardt
 * @created : 13.07.23
 **/
@Getter
@Setter
public class AuditedClientRepresentation extends ClientRepresentation {

    private String lastLogin;
}
