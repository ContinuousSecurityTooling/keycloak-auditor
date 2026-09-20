package net.cst.keycloak.resources;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import net.cst.keycloak.audit.model.AuditedClientRepresentation;
import net.cst.keycloak.audit.model.AuditedUserRepresentation;
import net.cst.keycloak.audit.model.ConfigConstants;
import net.cst.keycloak.events.logging.util.ClientModelHelper;
import net.cst.keycloak.events.logging.util.UserModelHelper;
import net.cst.keycloak.utils.ConfigHelper;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AuthenticationManager;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

            assertThrows(ForbiddenException.class, () -> endpoint.listUsers(headers, null, null));
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

            assertThrows(NotAuthorizedException.class, () -> endpoint.listUsers(headers, null, null));
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

            assertThrows(ForbiddenException.class, () -> endpoint.listUsers(headers, null, null));
        }
    }

    @Test
    void shouldAuthenticateViaSessionCookieWhenBearerMissingAndRoleGranted() {
        HttpHeaders headers = headersWith("X-Audit-Use-Session", "1");
        when(headers.getRequestHeader("x-forwarded-host")).thenReturn(List.of());

        RealmModel realm = mock(RealmModel.class);
        when(realm.getName()).thenReturn("master");
        RoleModel role = mock(RoleModel.class);
        String roleName = ConfigHelper.getConfigValue(ConfigConstants.DEFAULT_ROLE);
        when(realm.getRole(roleName)).thenReturn(role);

        UserModel user = mock(UserModel.class);
        when(user.hasRole(role)).thenReturn(true);

        KeycloakContext context = mock(KeycloakContext.class);
        when(context.getRequestHeaders()).thenReturn(headers);
        when(context.getRealm()).thenReturn(realm);
        KeycloakSession session = mock(KeycloakSession.class);
        when(session.getContext()).thenReturn(context);

        org.keycloak.models.RealmProvider realmProvider = mock(org.keycloak.models.RealmProvider.class);
        when(realmProvider.getRealmByName("master")).thenReturn(realm);
        when(session.realms()).thenReturn(realmProvider);

        org.keycloak.models.UserProvider userProvider = mock(org.keycloak.models.UserProvider.class);
        when(userProvider.searchForUserStream(eq(realm), anyMap())).thenReturn(java.util.stream.Stream.empty());
        when(session.users()).thenReturn(userProvider);

        AccessToken cookieToken = new AccessToken();
        cookieToken.issuer("http://localhost/realms/master");
        AuthenticationManager.AuthResult authResult =
                new AuthenticationManager.AuthResult(user, null, cookieToken, null);

        try (MockedStatic<Tokens> tokenMock = mockStatic(Tokens.class);
             MockedStatic<AuthenticationManager> authManagerMock = mockStatic(AuthenticationManager.class)) {
            tokenMock.when(() -> Tokens.getAccessToken(session)).thenReturn(null);
            authManagerMock.when(() -> AuthenticationManager.authenticateIdentityCookie(session, realm, true))
                    .thenReturn(authResult);

            AuditEndpoint endpoint = new AuditEndpoint(session) {
                @Override
                public void authenticate() {
                    // no-op for unit tests
                }
            };

            assertDoesNotThrow(() -> endpoint.listUsers(headers, null, null));
        }
    }

    @Test
    void shouldRejectSessionCookieUserWithoutRequiredRole() {
        HttpHeaders headers = headersWith("X-Audit-Use-Session", "1");
        when(headers.getRequestHeader("x-forwarded-host")).thenReturn(List.of());

        RealmModel realm = mock(RealmModel.class);
        when(realm.getName()).thenReturn("master");
        RoleModel role = mock(RoleModel.class);
        String roleName = ConfigHelper.getConfigValue(ConfigConstants.DEFAULT_ROLE);
        when(realm.getRole(roleName)).thenReturn(role);

        UserModel user = mock(UserModel.class);
        when(user.hasRole(role)).thenReturn(false);

        KeycloakContext context = mock(KeycloakContext.class);
        when(context.getRequestHeaders()).thenReturn(headers);
        when(context.getRealm()).thenReturn(realm);
        KeycloakSession session = mock(KeycloakSession.class);
        when(session.getContext()).thenReturn(context);

        AccessToken cookieToken = new AccessToken();
        cookieToken.issuer("http://localhost/realms/master");
        AuthenticationManager.AuthResult authResult =
                new AuthenticationManager.AuthResult(user, null, cookieToken, null);

        try (MockedStatic<Tokens> tokenMock = mockStatic(Tokens.class);
             MockedStatic<AuthenticationManager> authManagerMock = mockStatic(AuthenticationManager.class)) {
            tokenMock.when(() -> Tokens.getAccessToken(session)).thenReturn(null);
            authManagerMock.when(() -> AuthenticationManager.authenticateIdentityCookie(session, realm, true))
                    .thenReturn(authResult);

            AuditEndpoint endpoint = new AuditEndpoint(session) {
                @Override
                public void authenticate() {
                    // no-op for unit tests
                }
            };

            assertThrows(ForbiddenException.class, () -> endpoint.listUsers(headers, null, null));
        }
    }

    @Test
    void shouldNotAttemptSessionCookieFallbackWithoutOptInHeader() {
        HttpHeaders headers = headersWithout("x-forwarded-host");
        KeycloakSession session = sessionWithContextHeaders(headers);

        try (MockedStatic<Tokens> tokenMock = mockStatic(Tokens.class);
             MockedStatic<AuthenticationManager> authManagerMock = mockStatic(AuthenticationManager.class)) {
            tokenMock.when(() -> Tokens.getAccessToken(session)).thenReturn(null);

            AuditEndpoint endpoint = new AuditEndpoint(session) {
                @Override
                public void authenticate() {
                    // no-op for unit tests
                }
            };

            assertThrows(NotAuthorizedException.class, () -> endpoint.listUsers(headers, null, null));
            authManagerMock.verifyNoInteractions();
        }
    }

    @Test
    @SetEnvironmentVariable(key = "KC_AUD_DISABLE_EXTERNAL_ACCESS", value = "true")
    void shouldAllowAccessWhenExternalAccessDisabledButNoForwardedHostHeader() {
        HttpHeaders headers = headersWithout("x-forwarded-host");
        RealmModel realm = mock(RealmModel.class);
        KeycloakSession session = mockSessionWithEmptyRealm(headers, realm, "master");

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

            assertDoesNotThrow(() -> endpoint.listUsers(headers, null, null));
        }
    }

    @Test
    @SetEnvironmentVariable(key = "KC_AUD_DISABLE_ROLE_CHECK", value = "true")
    void shouldSkipRoleCheckWhenRoleCheckDisabled() {
        HttpHeaders headers = headersWithout("x-forwarded-host");
        RealmModel realm = mock(RealmModel.class);
        KeycloakSession session = mockSessionWithEmptyRealm(headers, realm, "master");

        AccessToken token = new AccessToken();
        token.issuer("http://localhost/realms/master");
        // no realmAccess/role at all - would normally be rejected

        try (MockedStatic<Tokens> tokenMock = mockStatic(Tokens.class)) {
            tokenMock.when(() -> Tokens.getAccessToken(session)).thenReturn(token);

            AuditEndpoint endpoint = new AuditEndpoint(session) {
                @Override
                public void authenticate() {
                    // no-op for unit tests
                }
            };

            assertDoesNotThrow(() -> endpoint.listUsers(headers, null, null));
        }
    }

    @Test
    void shouldRejectWhenBearerTokenHasNoRealmAccessAtAll() {
        HttpHeaders headers = headersWithout("x-forwarded-host");
        KeycloakSession session = sessionWithContextHeaders(headers);

        AccessToken token = new AccessToken();
        token.issuer("http://localhost/realms/master");
        // setRealmAccess() never called - stays null, distinct from an empty Access

        try (MockedStatic<Tokens> tokenMock = mockStatic(Tokens.class)) {
            tokenMock.when(() -> Tokens.getAccessToken(session)).thenReturn(token);

            AuditEndpoint endpoint = new AuditEndpoint(session) {
                @Override
                public void authenticate() {
                    // no-op for unit tests
                }
            };

            assertThrows(ForbiddenException.class, () -> endpoint.listUsers(headers, null, null));
        }
    }

    @Test
    void shouldNotAttemptSessionCookieFallbackWhenOptInHeaderPresentButEmpty() {
        HttpHeaders headers = mock(HttpHeaders.class);
        when(headers.getRequestHeader("X-Audit-Use-Session")).thenReturn(List.of());
        when(headers.getRequestHeader("x-forwarded-host")).thenReturn(List.of());
        when(headers.getRequestHeaders()).thenReturn(new MultivaluedHashMap<>());
        KeycloakSession session = sessionWithContextHeaders(headers);

        try (MockedStatic<Tokens> tokenMock = mockStatic(Tokens.class);
             MockedStatic<AuthenticationManager> authManagerMock = mockStatic(AuthenticationManager.class)) {
            tokenMock.when(() -> Tokens.getAccessToken(session)).thenReturn(null);

            AuditEndpoint endpoint = new AuditEndpoint(session) {
                @Override
                public void authenticate() {
                    // no-op for unit tests
                }
            };

            assertThrows(NotAuthorizedException.class, () -> endpoint.listUsers(headers, null, null));
            authManagerMock.verifyNoInteractions();
        }
    }

    @Test
    void shouldRejectSessionCookieFallbackWhenRealmMissing() {
        HttpHeaders headers = headersWith("X-Audit-Use-Session", "1");
        when(headers.getRequestHeader("x-forwarded-host")).thenReturn(List.of());

        KeycloakContext context = mock(KeycloakContext.class);
        when(context.getRequestHeaders()).thenReturn(headers);
        when(context.getRealm()).thenReturn(null);
        KeycloakSession session = mock(KeycloakSession.class);
        when(session.getContext()).thenReturn(context);

        try (MockedStatic<Tokens> tokenMock = mockStatic(Tokens.class);
             MockedStatic<AuthenticationManager> authManagerMock = mockStatic(AuthenticationManager.class)) {
            tokenMock.when(() -> Tokens.getAccessToken(session)).thenReturn(null);

            AuditEndpoint endpoint = new AuditEndpoint(session) {
                @Override
                public void authenticate() {
                    // no-op for unit tests
                }
            };

            assertThrows(NotAuthorizedException.class, () -> endpoint.listUsers(headers, null, null));
            authManagerMock.verifyNoInteractions();
        }
    }

    @Test
    void shouldRejectSessionCookieFallbackWhenIdentityCookieInvalid() {
        HttpHeaders headers = headersWith("X-Audit-Use-Session", "1");
        when(headers.getRequestHeader("x-forwarded-host")).thenReturn(List.of());

        RealmModel realm = mock(RealmModel.class);
        KeycloakContext context = mock(KeycloakContext.class);
        when(context.getRequestHeaders()).thenReturn(headers);
        when(context.getRealm()).thenReturn(realm);
        KeycloakSession session = mock(KeycloakSession.class);
        when(session.getContext()).thenReturn(context);

        try (MockedStatic<Tokens> tokenMock = mockStatic(Tokens.class);
             MockedStatic<AuthenticationManager> authManagerMock = mockStatic(AuthenticationManager.class)) {
            tokenMock.when(() -> Tokens.getAccessToken(session)).thenReturn(null);
            authManagerMock.when(() -> AuthenticationManager.authenticateIdentityCookie(session, realm, true))
                    .thenReturn(null);

            AuditEndpoint endpoint = new AuditEndpoint(session) {
                @Override
                public void authenticate() {
                    // no-op for unit tests
                }
            };

            assertThrows(NotAuthorizedException.class, () -> endpoint.listUsers(headers, null, null));
        }
    }

    @Test
    void shouldRejectSessionCookieUserWhenRoleNotConfiguredInRealm() {
        HttpHeaders headers = headersWith("X-Audit-Use-Session", "1");
        when(headers.getRequestHeader("x-forwarded-host")).thenReturn(List.of());

        RealmModel realm = mock(RealmModel.class);
        when(realm.getRole(anyString())).thenReturn(null); // role not configured in this realm

        UserModel user = mock(UserModel.class);

        KeycloakContext context = mock(KeycloakContext.class);
        when(context.getRequestHeaders()).thenReturn(headers);
        when(context.getRealm()).thenReturn(realm);
        KeycloakSession session = mock(KeycloakSession.class);
        when(session.getContext()).thenReturn(context);

        AccessToken cookieToken = new AccessToken();
        cookieToken.issuer("http://localhost/realms/master");
        AuthenticationManager.AuthResult authResult =
                new AuthenticationManager.AuthResult(user, null, cookieToken, null);

        try (MockedStatic<Tokens> tokenMock = mockStatic(Tokens.class);
             MockedStatic<AuthenticationManager> authManagerMock = mockStatic(AuthenticationManager.class)) {
            tokenMock.when(() -> Tokens.getAccessToken(session)).thenReturn(null);
            authManagerMock.when(() -> AuthenticationManager.authenticateIdentityCookie(session, realm, true))
                    .thenReturn(authResult);

            AuditEndpoint endpoint = new AuditEndpoint(session) {
                @Override
                public void authenticate() {
                    // no-op for unit tests
                }
            };

            assertThrows(ForbiddenException.class, () -> endpoint.listUsers(headers, null, null));
        }
    }

    @Test
    void listUsersShouldTreatBlankRealmFilterAsNoFilter() {
        HttpHeaders headers = headersWithout("x-forwarded-host");
        RealmModel masterRealm = mock(RealmModel.class);
        RealmModel otherRealm = mock(RealmModel.class);
        KeycloakContext context = mock(KeycloakContext.class);
        when(context.getRequestHeaders()).thenReturn(headers);
        when(context.getRealm()).thenReturn(masterRealm);
        KeycloakSession session = mock(KeycloakSession.class);
        when(session.getContext()).thenReturn(context);

        RealmProvider realmProvider = mock(RealmProvider.class);
        when(realmProvider.getRealmsStream()).thenReturn(Stream.of(masterRealm, otherRealm));
        when(session.realms()).thenReturn(realmProvider);

        UserModel masterUser = UserModelHelper.buildUser("1");
        UserModel otherUser = UserModelHelper.buildUser("2");
        UserProvider userProvider = mock(UserProvider.class);
        when(userProvider.searchForUserStream(eq(masterRealm), anyMap())).thenReturn(Stream.of(masterUser));
        when(userProvider.searchForUserStream(eq(otherRealm), anyMap())).thenReturn(Stream.of(otherUser));
        when(session.users()).thenReturn(userProvider);

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

            List<AuditedUserRepresentation> users = endpoint.listUsers(headers, null, "   ");
            assertEquals(2, users.size(), "Blank realm filter should be treated as no filter (all realms)");
        }
    }

    @Test
    void listUsersShouldReturnEmptyListWhenRealmFilterMatchesNoRealm() {
        // Pins current behavior: an unknown ?realm= silently yields no results rather than a
        // 404/error, since shouldIncludeAllRealms() takes the filtered branch but
        // getRealmByName() returns null and the "if (targetRealm != null)" guard just skips it.
        HttpHeaders headers = headersWithout("x-forwarded-host");
        RealmModel masterRealm = mock(RealmModel.class);
        KeycloakContext context = mock(KeycloakContext.class);
        when(context.getRequestHeaders()).thenReturn(headers);
        when(context.getRealm()).thenReturn(masterRealm);
        KeycloakSession session = mock(KeycloakSession.class);
        when(session.getContext()).thenReturn(context);

        RealmProvider realmProvider = mock(RealmProvider.class);
        when(realmProvider.getRealmByName("does-not-exist")).thenReturn(null);
        when(session.realms()).thenReturn(realmProvider);

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

            List<AuditedUserRepresentation> users = endpoint.listUsers(headers, null, "does-not-exist");
            assertEquals(0, users.size(), "Unknown realm filter should yield an empty list, not an error");
        }
    }

    @Test
    void listClientsShouldTreatBlankRealmFilterAsNoFilter() {
        HttpHeaders headers = headersWithout("x-forwarded-host");
        RealmModel masterRealm = mock(RealmModel.class);
        RealmModel otherRealm = mock(RealmModel.class);
        KeycloakContext context = mock(KeycloakContext.class);
        when(context.getRequestHeaders()).thenReturn(headers);
        when(context.getRealm()).thenReturn(masterRealm);
        KeycloakSession session = mock(KeycloakSession.class);
        when(session.getContext()).thenReturn(context);

        RealmProvider realmProvider = mock(RealmProvider.class);
        when(realmProvider.getRealmsStream()).thenReturn(Stream.of(masterRealm, otherRealm));
        when(session.realms()).thenReturn(realmProvider);

        org.keycloak.common.Profile.configure();
        org.keycloak.authorization.AuthorizationProvider authorization = mock(org.keycloak.authorization.AuthorizationProvider.class);
        org.keycloak.authorization.store.StoreFactory store = mock(org.keycloak.authorization.store.StoreFactory.class);
        when(authorization.getStoreFactory()).thenReturn(store);
        when(store.getResourceServerStore()).thenReturn(mock(org.keycloak.authorization.store.ResourceServerStore.class));
        when(session.getProvider(org.keycloak.authorization.AuthorizationProvider.class)).thenReturn(authorization);

        ClientModel masterClient = ClientModelHelper.buildClient();
        ClientModel otherClient = ClientModelHelper.buildClient();
        ClientProvider clientProvider = mock(ClientProvider.class);
        when(clientProvider.getClientsStream(masterRealm)).thenReturn(Stream.of(masterClient));
        when(clientProvider.getClientsStream(otherRealm)).thenReturn(Stream.of(otherClient));
        when(session.clients()).thenReturn(clientProvider);

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

            List<AuditedClientRepresentation> clients = endpoint.listClients(headers, null, "   ");
            assertEquals(2, clients.size(), "Blank realm filter should be treated as no filter (all realms)");
        }
    }

    private static KeycloakSession mockSessionWithEmptyRealm(HttpHeaders headers, RealmModel realm, String realmName) {
        when(realm.getName()).thenReturn(realmName);

        KeycloakContext context = mock(KeycloakContext.class);
        when(context.getRequestHeaders()).thenReturn(headers);
        when(context.getRealm()).thenReturn(realm);
        KeycloakSession session = mock(KeycloakSession.class);
        when(session.getContext()).thenReturn(context);

        RealmProvider realmProvider = mock(RealmProvider.class);
        when(realmProvider.getRealmByName(realmName)).thenReturn(realm);
        when(session.realms()).thenReturn(realmProvider);

        UserProvider userProvider = mock(UserProvider.class);
        when(userProvider.searchForUserStream(eq(realm), anyMap())).thenReturn(Stream.empty());
        when(session.users()).thenReturn(userProvider);

        return session;
    }
}
