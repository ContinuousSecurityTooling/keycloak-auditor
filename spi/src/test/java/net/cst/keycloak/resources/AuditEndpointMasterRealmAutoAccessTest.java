package net.cst.keycloak.resources;

import net.cst.keycloak.audit.model.AuditedClientRepresentation;
import net.cst.keycloak.audit.model.AuditedUserRepresentation;
import net.cst.keycloak.utils.EndpointTest;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that a token issued by the master realm automatically includes all realms
 * in audit downloads, regardless of the KC_AUD_GLOBAL_MASTER_ACCESS flag.
 */
@SetEnvironmentVariable(key = "KC_AUD_GLOBAL_MASTER_ACCESS", value = "false")
class AuditEndpointMasterRealmAutoAccessTest extends EndpointTest {

    @Test
    void masterRealmTokenShouldIncludeUsersFromAllRealms() {
        List<AuditedUserRepresentation> users = getUsersViaEndpoint("master");
        assertNotNull(users);
        assertEquals(4, users.size(), "Master realm should include users from all realms, got " + users.size());
    }

    @Test
    void masterRealmTokenShouldIncludeClientsFromAllRealms() {
        List<AuditedClientRepresentation> clients = getClientsViaEndpoint("master");
        assertNotNull(clients);
        assertEquals(4, clients.size(), "Master realm should include clients from all realms, got " + clients.size());
    }
}
