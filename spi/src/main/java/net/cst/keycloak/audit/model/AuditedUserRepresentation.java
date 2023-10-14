package net.cst.keycloak.audit.model;

import lombok.Getter;
import lombok.Setter;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.HashMap;
import java.util.Map;

/**
 * @author : mreinhardt
 * @created : 13.07.23
 **/
@Getter
@Setter
public class AuditedUserRepresentation extends UserRepresentation {

    private String lastLogin;

    private String realm;

    private Map<String, String> clientLogins = new HashMap<>();

}
