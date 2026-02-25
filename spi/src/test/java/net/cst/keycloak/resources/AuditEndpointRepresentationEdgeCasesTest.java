package net.cst.keycloak.resources;

import net.cst.keycloak.audit.model.AuditedUserRepresentation;
import net.cst.keycloak.events.logging.util.UserModelHelper;
import org.junit.jupiter.api.Test;
import org.keycloak.models.UserModel;

import static org.junit.jupiter.api.Assertions.*;

class AuditEndpointRepresentationEdgeCasesTest {

    @Test
    void shouldSetNullsWhenNoLastLoginAttributesPresent() {
        UserModel source = UserModelHelper.buildUser("123"); // no last-login attribute
        AuditedUserRepresentation rep = AuditEndpoint.toBriefRepresentation(source, "master");

        assertNotNull(rep);
        assertEquals("123", rep.getId());
        assertNull(rep.getLastLogin());
        assertNull(rep.getClientLogins());
    }
}
