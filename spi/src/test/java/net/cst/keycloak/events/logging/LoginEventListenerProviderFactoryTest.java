package net.cst.keycloak.events.logging;

import net.cst.keycloak.utils.EndpointTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class LoginEventListenerProviderFactoryTest extends EndpointTest {

    @Test
    void shouldCreateProvider() {
        var given = new LoginEventListenerProviderFactory().create(session);
        assertNotNull(given);
    }

}
