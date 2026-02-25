package net.cst.keycloak.resources;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import net.cst.keycloak.audit.model.ConfigConstants;
import net.cst.keycloak.utils.ConfigHelper;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessToken;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AuditEndpointAccessRightsTest {

    private static HttpHeaders headersWith(String name, String value) {
        HttpHeaders headers = mock(HttpHeaders.class);
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.put(name, List.of(value));
        when(headers.getRequestHeader(name)).thenReturn(List.of(value));
        when(headers.getRequestHeaders()).thenReturn(map);
        return headers;
    }

    private static HttpHeaders headersWithout(String name) {
        HttpHeaders headers = mock(HttpHeaders.class);
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        when(headers.getRequestHeader(name)).thenReturn(List.of());
        when(headers.getRequestHeaders()).thenReturn(map);
        return headers;
    }

    private static KeycloakSession sessionWithContextHeaders(HttpHeaders headers) {
        KeycloakSession session = mock(KeycloakSession.class);
        KeycloakContext context = mock(KeycloakContext.class);
        when(context.getRequestHeaders()).thenReturn(headers);
        when(session.getContext()).thenReturn(context);
        return session;
    }

    @Test
    @SetEnvironmentVariable(key = "KC_AUD_DISABLE_EXTERNAL_ACCESS", value = "true")
    void shouldRejectExternalAccessWhenForwardedHostPresent() {
        HttpHeaders headers = headersWith("x-forwarded-host", "example.com");
        KeycloakSession session = sessionWithContextHeaders(headers);

        AccessToken token = new AccessToken();
        token.issuer("http://localhost/realms/master");
        token.setRealmAccess(new AccessToken.Access().addRole(ConfigHelper.getConfigValue(ConfigConstants.DEFAULT_ROLE)));

        try (MockedStatic<Tokens> tokenMock = mockStatic(Tokens.class)) {
            tokenMock.when(() -> Tokens.getAccessToken(session)).thenReturn(token);

            AuditEndpoint endpoint = new AuditEndpoint(session) {
                @Override
                public void authenticate() {
                    // no-op for unit tests
                }
            };

            assertThrows(ForbiddenException.class, () -> endpoint.listUsers(headers));
        }
    }

    @Test
    void shouldRejectWhenAuthTokenMissing() {
        HttpHeaders headers = headersWithout("x-forwarded-host");
        KeycloakSession session = sessionWithContextHeaders(headers);

        try (MockedStatic<Tokens> tokenMock = mockStatic(Tokens.class)) {
            tokenMock.when(() -> Tokens.getAccessToken(session)).thenReturn(null);

            AuditEndpoint endpoint = new AuditEndpoint(session) {
                @Override
                public void authenticate() {
                    // no-op for unit tests
                }
            };

            assertThrows(NotAuthorizedException.class, () -> endpoint.listUsers(headers));
        }
    }

    @Test
    @SetEnvironmentVariable(key = "KC_AUD_DISABLE_ROLE_CHECK", value = "false")
    @SetEnvironmentVariable(key = "KC_AUD_DEFAULT_ROLE", value = "admin")
    void shouldRejectWhenRoleMissingAndRoleCheckEnabled() {
        HttpHeaders headers = headersWithout("x-forwarded-host");
        KeycloakSession session = sessionWithContextHeaders(headers);

        AccessToken token = new AccessToken();
        token.issuer("http://localhost/realms/master");
        token.setRealmAccess(new AccessToken.Access()); // no roles

        try (MockedStatic<Tokens> tokenMock = mockStatic(Tokens.class)) {
            tokenMock.when(() -> Tokens.getAccessToken(session)).thenReturn(token);

            AuditEndpoint endpoint = new AuditEndpoint(session) {
                @Override
                public void authenticate() {
                    // no-op for unit tests
                }
            };

            assertThrows(ForbiddenException.class, () -> endpoint.listUsers(headers));
        }
    }
}
