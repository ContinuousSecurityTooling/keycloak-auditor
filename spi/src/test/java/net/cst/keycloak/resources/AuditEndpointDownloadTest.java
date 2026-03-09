package net.cst.keycloak.resources;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import net.cst.keycloak.audit.model.ConfigConstants;
import net.cst.keycloak.events.logging.util.ClientModelHelper;
import net.cst.keycloak.events.logging.util.UserModelHelper;
import net.cst.keycloak.utils.ConfigHelper;
import net.cst.keycloak.utils.EndpointTest;
import org.junit.jupiter.api.Test;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.models.*;
import org.keycloak.representations.AccessToken;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditEndpointDownloadTest extends EndpointTest {

    // -----------------------------------------------------------------------
    // HTML download page
    // -----------------------------------------------------------------------

    @Test
    void downloadPageShouldReturnHtmlWithoutRequiringAuth() {
        RealmModel masterRealm = mock(RealmModel.class);
        when(masterRealm.getName()).thenReturn("master");
        KeycloakContext context = mock(KeycloakContext.class);
        when(context.getRealm()).thenReturn(masterRealm);
        when(session.getContext()).thenReturn(context);

        try (MockedStatic<Tokens> tokenMock = mockStatic(Tokens.class)) {
            tokenMock.when(() -> Tokens.getAccessToken(session)).thenReturn(null);

            AuditEndpoint endpoint = new AuditEndpoint(session) {
                @Override
                public void authenticate() { /* no-op */ }
            };

            Response response = endpoint.downloadPage();

            assertEquals(200, response.getStatus());
            String html = (String) response.getEntity();
            assertTrue(html.contains("Keycloak Audit Reports"), "Page title missing");
            assertTrue(html.contains("download('users','csv'"), "Users CSV button missing");
            assertTrue(html.contains("download('clients','csv'"), "Clients CSV button missing");
            assertTrue(html.contains("download('users','json'"), "Users JSON button missing");
            assertTrue(html.contains("download('clients','json'"), "Clients JSON button missing");
            assertTrue(html.contains("autoDetect"), "Auto-detect JS function missing");
            assertTrue(html.contains("/csv"), "CSV path pattern missing in JS");
        }
    }

    // -----------------------------------------------------------------------
    // CSV downloads
    // -----------------------------------------------------------------------

    @Test
    void downloadUsersCsvShouldReturnCsvWithDispositionHeader() {
        Response response = getUsersCsvViaEndpoint();

        assertEquals(200, response.getStatus());
        String disposition = response.getHeaderString("Content-Disposition");
        assertNotNull(disposition);
        assertTrue(disposition.contains("attachment"));
        assertTrue(disposition.contains("audit-users-report.csv"));

        String body = (String) response.getEntity();
        assertTrue(body.startsWith("username,email,firstName,lastName,realm,lastLogin"),
                "CSV header row missing or incorrect");
    }

    @Test
    void downloadUsersCsvBodyShouldContainUserData() {
        Response response = getUsersCsvViaEndpoint();
        String body = (String) response.getEntity();

        // EndpointTest sets up 2 users in "master" realm (one with lastLogin, one without)
        String[] lines = body.split("\n");
        assertTrue(lines.length >= 2, "Expected at least a header + one data row");
    }

    @Test
    void downloadClientsCsvShouldReturnCsvWithDispositionHeader() {
        Response response = getClientsCsvViaEndpoint();

        assertEquals(200, response.getStatus());
        String disposition = response.getHeaderString("Content-Disposition");
        assertNotNull(disposition);
        assertTrue(disposition.contains("attachment"));
        assertTrue(disposition.contains("audit-clients-report.csv"));

        String body = (String) response.getEntity();
        assertTrue(body.startsWith("clientId,name,realm,lastLogin"),
                "CSV header row missing or incorrect");
    }

    @Test
    void downloadClientsCsvBodyShouldContainClientData() {
        Response response = getClientsCsvViaEndpoint();
        String body = (String) response.getEntity();

        String[] lines = body.split("\n");
        assertTrue(lines.length >= 2, "Expected at least a header + one data row");
    }

    // -----------------------------------------------------------------------
    // Helpers (mirror EndpointTest helpers but call the CSV endpoints)
    // -----------------------------------------------------------------------

    private Response getUsersCsvViaEndpoint() {
        HttpHeaders headers = headersWithAuth();
        RealmModel masterRealm = mock(RealmModel.class);
        RealmModel anotherRealm = mock(RealmModel.class);
        RealmProvider realmProvider = mock(RealmProvider.class);
        UserProvider userProvider = mock(UserProvider.class);

        Stream<UserModel> masterUsers = Arrays.stream(new UserModel[]{
                UserModelHelper.buildUser("1", "2024-06-15T09:07:19.45743358Z"),
                UserModelHelper.buildUser("2")
        });
        Stream<UserModel> otherUsers = Arrays.stream(new UserModel[]{
                UserModelHelper.buildUser("3"), UserModelHelper.buildUser("4")
        });

        when(userProvider.searchForUserStream(masterRealm, Map.of(UserModel.SEARCH, "*"))).thenReturn(masterUsers);
        when(userProvider.searchForUserStream(anotherRealm, Map.of(UserModel.SEARCH, "*"))).thenReturn(otherUsers);
        when(realmProvider.getRealmByName("master")).thenReturn(masterRealm);
        when(realmProvider.getRealmsStream()).thenReturn(Arrays.stream(new RealmModel[]{masterRealm, anotherRealm}));
        setupSessionContext(headers, masterRealm, realmProvider);
        when(session.users()).thenReturn(userProvider);

        try (MockedStatic<Tokens> tokenMock = mockStatic(Tokens.class)) {
            auditEndpoint = mockAccessTokenEndpoint(tokenMock);
            return auditEndpoint.downloadUsersCsv(headers);
        }
    }

    private Response getClientsCsvViaEndpoint() {
        HttpHeaders headers = headersWithAuth();
        RealmModel masterRealm = mock(RealmModel.class);
        RealmModel anotherRealm = mock(RealmModel.class);
        RealmProvider realmProvider = mock(RealmProvider.class);
        ClientProvider clientProvider = mock(ClientProvider.class);

        Stream<ClientModel> masterClients = Arrays.stream(new ClientModel[]{
                ClientModelHelper.buildClient(), ClientModelHelper.buildClient()
        });
        Stream<ClientModel> otherClients = Arrays.stream(new ClientModel[]{
                ClientModelHelper.buildClient()
        });

        when(clientProvider.getClientsStream(masterRealm)).thenReturn(masterClients);
        when(clientProvider.getClientsStream(anotherRealm)).thenReturn(otherClients);
        when(realmProvider.getRealmByName("master")).thenReturn(masterRealm);
        when(realmProvider.getRealmsStream()).thenReturn(Arrays.stream(new RealmModel[]{masterRealm, anotherRealm}));
        setupSessionContext(headers, masterRealm, realmProvider);
        when(session.clients()).thenReturn(clientProvider);

        try (MockedStatic<Tokens> tokenMock = mockStatic(Tokens.class)) {
            auditEndpoint = mockAccessTokenEndpoint(tokenMock);
            return auditEndpoint.downloadClientsCsv(headers);
        }
    }

    private HttpHeaders headersWithAuth() {
        HttpHeaders headers = mock(HttpHeaders.class);
        MultivaluedMap<String, String> headerValues = new MultivaluedHashMap<>();
        headerValues.put(HttpHeaders.AUTHORIZATION, List.of("BEARER 1234"));
        when(headers.getRequestHeaders()).thenReturn(headerValues);
        when(headers.getRequestHeader("x-forwarded-host")).thenReturn(List.of());
        return headers;
    }

    private void setupSessionContext(HttpHeaders headers, RealmModel realm, RealmProvider realmProvider) {
        KeycloakContext context = mock(KeycloakContext.class);
        when(context.getRealm()).thenReturn(realm);
        when(context.getRequestHeaders()).thenReturn(headers);
        when(session.getContext()).thenReturn(context);
        when(session.realms()).thenReturn(realmProvider);
    }

    private AuditEndpoint mockAccessTokenEndpoint(MockedStatic<Tokens> tokenMock) {
        AccessToken token = new AccessToken();
        token.issuer("master");
        token.setRealmAccess(new AccessToken.Access()
                .addRole(ConfigHelper.getConfigValue(ConfigConstants.DEFAULT_ROLE)));
        tokenMock.when(() -> Tokens.getAccessToken(session)).thenReturn(token);
        return new AuditEndpoint(session) {
            @Override
            public void authenticate() { /* no-op */ }
        };
    }
}
