package net.cst.keycloak.resources;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import net.cst.keycloak.audit.model.AuditedClientRepresentation;
import net.cst.keycloak.audit.model.AuditedUserRepresentation;
import net.cst.keycloak.audit.model.ConfigConstants;
import net.cst.keycloak.events.logging.util.ClientModelHelper;
import net.cst.keycloak.events.logging.util.UserModelHelper;
import net.cst.keycloak.utils.ConfigHelper;
import net.cst.keycloak.utils.EndpointTest;
import org.junit.jupiter.api.Test;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.models.*;
import org.keycloak.models.ClientModel;
import org.keycloak.representations.AccessToken;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the ?realm= filter parameter on listUsers, listClients, and CSV endpoints.
 */
class AuditEndpointRealmFilterTest extends EndpointTest {

    // -----------------------------------------------------------------------
    // Users
    // -----------------------------------------------------------------------

    @Test
    void listUsersShouldReturnOnlyFilteredRealmWhenRealmFilterProvided() {
        List<AuditedUserRepresentation> users = getUsersWithRealmFilter("master", "target-realm");
        assertNotNull(users);
        // target-realm has 2 users
        assertEquals(2, users.size());
        users.forEach(u -> assertEquals("target-realm", u.getRealm()));
    }

    @Test
    void listUsersShouldReturnEmptyWhenRealmFilterDoesNotExist() {
        List<AuditedUserRepresentation> users = getUsersWithRealmFilter("master", "nonexistent");
        assertNotNull(users);
        assertTrue(users.isEmpty(), "Unknown realm filter should return empty list");
    }

    @Test
    void listUsersShouldIgnoreRealmFilterForNonMasterToken() {
        // non-master issuer → shouldIncludeAllRealms=false → realmFilter is irrelevant
        List<AuditedUserRepresentation> users = getUsersWithRealmFilter("other", "target-realm");
        assertNotNull(users);
        // non-master falls through to current-realm path (2 users from "other" realm)
        assertEquals(2, users.size());
    }

    // -----------------------------------------------------------------------
    // Clients
    // -----------------------------------------------------------------------

    @Test
    void listClientsShouldReturnOnlyFilteredRealm() {
        List<AuditedClientRepresentation> clients = getClientsWithRealmFilter("master", "target-realm");
        assertNotNull(clients);
        assertEquals(2, clients.size());
        clients.forEach(c -> assertEquals("target-realm", c.getRealm()));
    }

    @Test
    void listClientsShouldReturnEmptyWhenRealmFilterDoesNotExist() {
        List<AuditedClientRepresentation> clients = getClientsWithRealmFilter("master", "nonexistent");
        assertNotNull(clients);
        assertTrue(clients.isEmpty());
    }

    // -----------------------------------------------------------------------
    // CSV endpoints
    // -----------------------------------------------------------------------

    @Test
    void downloadUsersCsvShouldRespectRealmFilter() {
        HttpHeaders headers = mockAuthHeaders();
        setupRealmSession(headers, "master");

        try (MockedStatic<Tokens> tokenMock = mockStatic(Tokens.class)) {
            auditEndpoint = buildEndpoint(tokenMock, "master");
            Response resp = auditEndpoint.downloadUsersCsv(headers, null, "target-realm");
            assertEquals(200, resp.getStatus());
            String body = (String) resp.getEntity();
            assertTrue(body.contains("target-realm"), "CSV should contain target-realm column");
        }
    }

    @Test
    void downloadClientsCsvShouldRespectRealmFilter() {
        HttpHeaders headers = mockAuthHeaders();
        setupRealmSession(headers, "master");

        try (MockedStatic<Tokens> tokenMock = mockStatic(Tokens.class)) {
            auditEndpoint = buildEndpoint(tokenMock, "master");
            Response resp = auditEndpoint.downloadClientsCsv(headers, null, "target-realm");
            assertEquals(200, resp.getStatus());
            String body = (String) resp.getEntity();
            assertTrue(body.contains("target-realm"), "CSV should contain target-realm column");
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private List<AuditedUserRepresentation> getUsersWithRealmFilter(String issuerRealm, String realmFilter) {
        HttpHeaders headers = mockAuthHeaders();
        setupRealmSession(headers, issuerRealm);
        try (MockedStatic<Tokens> tokenMock = mockStatic(Tokens.class)) {
            auditEndpoint = buildEndpoint(tokenMock, issuerRealm);
            return auditEndpoint.listUsers(headers, null, realmFilter);
        }
    }

    private List<AuditedClientRepresentation> getClientsWithRealmFilter(String issuerRealm, String realmFilter) {
        HttpHeaders headers = mockAuthHeaders();
        setupRealmSession(headers, issuerRealm);
        try (MockedStatic<Tokens> tokenMock = mockStatic(Tokens.class)) {
            auditEndpoint = buildEndpoint(tokenMock, issuerRealm);
            return auditEndpoint.listClients(headers, null, realmFilter);
        }
    }

    private HttpHeaders mockAuthHeaders() {
        HttpHeaders headers = mock(HttpHeaders.class);
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.put(HttpHeaders.AUTHORIZATION, List.of("BEARER 1234"));
        when(headers.getRequestHeaders()).thenReturn(map);
        when(headers.getRequestHeader("x-forwarded-host")).thenReturn(List.of());
        return headers;
    }

    private void setupRealmSession(HttpHeaders headers, String issuerRealm) {
        RealmModel masterRealm = mock(RealmModel.class);
        RealmModel targetRealm = mock(RealmModel.class);
        RealmModel otherRealm = mock(RealmModel.class);
        when(masterRealm.getName()).thenReturn("master");
        when(targetRealm.getName()).thenReturn("target-realm");
        when(otherRealm.getName()).thenReturn("other");

        RealmProvider realmProvider = mock(RealmProvider.class);
        when(realmProvider.getRealmsStream()).thenReturn(Arrays.stream(new RealmModel[]{masterRealm, targetRealm}));
        when(realmProvider.getRealmByName("master")).thenReturn(masterRealm);
        when(realmProvider.getRealmByName("target-realm")).thenReturn(targetRealm);
        when(realmProvider.getRealmByName("other")).thenReturn(otherRealm);
        when(realmProvider.getRealmByName("nonexistent")).thenReturn(null);

        // Build model streams before stubbing to avoid Mockito unfinished-stubbing false positives
        UserModel u1 = UserModelHelper.buildUser("1"), u2 = UserModelHelper.buildUser("2");
        UserModel u3 = UserModelHelper.buildUser("3"), u4 = UserModelHelper.buildUser("4");
        UserModel u5 = UserModelHelper.buildUser("5"), u6 = UserModelHelper.buildUser("6");
        ClientModel c1 = ClientModelHelper.buildClient(), c2 = ClientModelHelper.buildClient();
        ClientModel c3 = ClientModelHelper.buildClient(), c4 = ClientModelHelper.buildClient();
        ClientModel c5 = ClientModelHelper.buildClient(), c6 = ClientModelHelper.buildClient();

        UserProvider userProvider = mock(UserProvider.class);
        when(userProvider.searchForUserStream(masterRealm, Map.of(UserModel.SEARCH, "*")))
                .thenReturn(Arrays.stream(new UserModel[]{u1, u2}));
        when(userProvider.searchForUserStream(targetRealm, Map.of(UserModel.SEARCH, "*")))
                .thenReturn(Arrays.stream(new UserModel[]{u3, u4}));
        when(userProvider.searchForUserStream(otherRealm, Map.of(UserModel.SEARCH, "*")))
                .thenReturn(Arrays.stream(new UserModel[]{u5, u6}));

        ClientProvider clientProvider = mock(ClientProvider.class);
        when(clientProvider.getClientsStream(masterRealm))
                .thenReturn(Arrays.stream(new ClientModel[]{c1, c2}));
        when(clientProvider.getClientsStream(targetRealm))
                .thenReturn(Arrays.stream(new ClientModel[]{c3, c4}));
        when(clientProvider.getClientsStream(otherRealm))
                .thenReturn(Arrays.stream(new ClientModel[]{c5, c6}));

        KeycloakContext context = mock(KeycloakContext.class);
        when(context.getRealm()).thenReturn("master".equals(issuerRealm) ? masterRealm : otherRealm);
        when(context.getRequestHeaders()).thenReturn(headers);
        when(session.getContext()).thenReturn(context);
        when(session.realms()).thenReturn(realmProvider);
        when(session.users()).thenReturn(userProvider);
        when(session.clients()).thenReturn(clientProvider);
    }

    private AuditEndpoint buildEndpoint(MockedStatic<Tokens> tokenMock, String issuerRealm) {
        AccessToken token = new AccessToken();
        token.issuer(issuerRealm);
        token.setRealmAccess(new AccessToken.Access()
                .addRole(ConfigHelper.getConfigValue(ConfigConstants.DEFAULT_ROLE)));
        tokenMock.when(() -> Tokens.getAccessToken(session)).thenReturn(token);
        return new AuditEndpoint(session) {
            @Override
            public void authenticate() { /* no-op */ }
        };
    }
}
