package net.cst.keycloak.resources;

import net.cst.keycloak.audit.model.AuditedClientRepresentation;
import net.cst.keycloak.audit.model.AuditedUserRepresentation;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author : mreinhardt
 * @created : 09.08.23
 **/
@SetEnvironmentVariable(key = "KC_AUD_GLOBAL_MASTER_ACCESS", value = "false")
class AuditEndpointWithoutMasterAccessTest extends AuditEndpointTest {

    @Test
    void shouldNotShowUsersFromAllRealmsIfNotConfigured() throws Exception {
        List<AuditedUserRepresentation> usersResponse = getUsersViaEndpoint();
        assertNotNull(usersResponse);
        assertTrue(usersResponse.size() == 2, "Expected 2 users, but got " + usersResponse.size());
    }

    @Test
    void shouldNotShowClientsFromAllRealmsIfConfigured() throws Exception {
        List<AuditedClientRepresentation> clients = getClientsViaEndpoint();
        assertNotNull(clients);
        assertTrue(clients.size() == 2, "Expected 2 clients, but got " + clients.size());
    }
}
