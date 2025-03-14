package net.cst.keycloak.resources;

import net.cst.keycloak.audit.model.AuditedClientRepresentation;
import net.cst.keycloak.audit.model.AuditedUserRepresentation;
import net.cst.keycloak.utils.EndpointTest;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuditEndpointWithoutMasterAccessTest extends EndpointTest {

    @Test
    @SetEnvironmentVariable(key = "KC_AUD_GLOBAL_MASTER_ACCESS", value = "false")
    void shouldNotShowUsersFromAllRealmsIfNotConfigured() {
        List<AuditedUserRepresentation> usersResponse = getUsersViaEndpoint();
        auditEndpoint.authenticate();
        assertNotNull(usersResponse);
        assertEquals(2, usersResponse.size(), "Expected 2 users, but got " + usersResponse.size());
    }

    @Test
    @SetEnvironmentVariable(key = "KC_AUD_GLOBAL_MASTER_ACCESS", value = "false")
    void shouldNotShowClientsFromAllRealmsIfConfigured() {
        List<AuditedClientRepresentation> clients = getClientsViaEndpoint();
        auditEndpoint.authenticate();
        assertNotNull(clients);
        assertEquals(2, clients.size(), "Expected 2 clients, but got " + clients.size());
    }
}
