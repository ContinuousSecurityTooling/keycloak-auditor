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
@SetEnvironmentVariable(key = "KC_AUD_GLOBAL_MASTER_ACCESS", value = "true")
class AuditEndpointWithMasterAccessTest extends AuditEndpointTest {

    @Test
    void shouldShowUsersFromAllRealmsIfConfigured() throws Exception {
        List<AuditedUserRepresentation> users = getUsersViaEndpoint();
        assertNotNull(users);
        assertTrue(users.size() == 4, "Expected 4 users, but got " + users.size());
    }

    @Test
    void shouldShowClientsFromAllRealmsIfConfigured() throws Exception {
        List<AuditedClientRepresentation> clients = getClientsViaEndpoint();
        assertNotNull(clients);
        assertTrue(clients.size() == 4, "Expected 4 clients, but got " + clients.size());
    }
}
