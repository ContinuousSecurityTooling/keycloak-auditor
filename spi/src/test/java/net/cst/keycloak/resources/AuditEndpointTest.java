package net.cst.keycloak.resources;

import net.cst.keycloak.audit.model.AuditedClientRepresentation;
import net.cst.keycloak.audit.model.AuditedUserRepresentation;
import net.cst.keycloak.events.logging.util.ClientModelHelper;
import net.cst.keycloak.events.logging.util.UserModelHelper;
import net.cst.keycloak.utils.EndpointTest;
import org.junit.jupiter.api.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuditEndpointTest extends EndpointTest {

    @Test
    void shouldConvertToAuditedClientRepresentation() {
        ClientModel source = ClientModelHelper.buildClient();
        AuditedClientRepresentation client = AuditEndpoint.toBriefRepresentation(source, "test", session);
        assertNotNull(client);
        assertEquals(client.getClientId(), source.getClientId());
    }

    @Test
    void shouldConvertToAuditedUserRepresentation() {
        UserModel source = UserModelHelper.buildUser();
        AuditedUserRepresentation user = AuditEndpoint.toBriefRepresentation(source, "test");
        assertNotNull(user);
        assertEquals(user.getId(), source.getId());
    }
}
