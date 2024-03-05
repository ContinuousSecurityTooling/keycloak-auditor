package net.cst.keycloak.resources;

import net.cst.keycloak.audit.model.AuditedClientRepresentation;
import net.cst.keycloak.audit.model.AuditedUserRepresentation;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        assertEquals(4, users.size(), "Expected 4 users, but got " + users.size());
    }

    @Test
    void shouldShowClientsFromAllRealmsIfConfigured() throws Exception {
        List<AuditedClientRepresentation> clients = getClientsViaEndpoint();
        assertNotNull(clients);
        assertEquals(4, clients.size(), "Expected 4 clients, but got " + clients.size());
    }
}
