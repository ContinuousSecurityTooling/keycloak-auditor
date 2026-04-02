package net.cst.keycloak.audit.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class LoginDetailsTest {

    @Test
    void clientLoginDetailsShouldGetAndSetKcLogin() {
        ClientLoginDetails details = new ClientLoginDetails();
        assertNull(details.getKcLogin());
        OffsetDateTime now = OffsetDateTime.now();
        details.setKcLogin(now);
        assertEquals(now, details.getKcLogin());
    }

    @Test
    void userLoginDetailsShouldGetAndSetKcLogin() {
        UserLoginDetails details = new UserLoginDetails();
        assertNull(details.getKcLogin());
        OffsetDateTime now = OffsetDateTime.now();
        details.setKcLogin(now);
        assertEquals(now, details.getKcLogin());
    }

    @Test
    void userLoginDetailsShouldHaveMutableClientLoginsMap() {
        UserLoginDetails details = new UserLoginDetails();
        assertNotNull(details.getClientLogins());
        assertTrue(details.getClientLogins().isEmpty());
        OffsetDateTime now = OffsetDateTime.now();
        details.getClientLogins().put("my-client", now);
        assertEquals(now, details.getClientLogins().get("my-client"));
    }

    @Test
    void userLoginDetailsShouldSetClientLogins() {
        UserLoginDetails details = new UserLoginDetails();
        java.util.Map<String, OffsetDateTime> map = new java.util.HashMap<>();
        map.put("client-a", OffsetDateTime.now());
        details.setClientLogins(map);
        assertEquals(map, details.getClientLogins());
    }
}
