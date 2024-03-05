package net.cst.keycloak.resources;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import net.cst.keycloak.audit.model.AuditedClientRepresentation;
import net.cst.keycloak.audit.model.AuditedUserRepresentation;
import net.cst.keycloak.events.logging.util.ClientModelHelper;
import net.cst.keycloak.events.logging.util.UserModelHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.common.Profile;
import org.keycloak.models.*;
import org.keycloak.representations.AccessToken;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * @author : mreinhardt
 * @created : 09.08.23
 **/
class AuditEndpointTest {

    static KeycloakSession session;

    @BeforeEach
    void setUp() {
        session = mock(KeycloakSession.class);
        AuthorizationProvider authorization = mock(AuthorizationProvider.class);
        StoreFactory store = mock(StoreFactory.class);
        ResourceServerStore resourceServerStore = mock(ResourceServerStore.class);
        when(authorization.getStoreFactory()).thenReturn(store);
        when(store.getResourceServerStore()).thenReturn(resourceServerStore);
        when(session.getProvider(AuthorizationProvider.class)).thenReturn(authorization);
        Profile.configure();
    }

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

    protected static List<AuditedUserRepresentation> getUsersViaEndpoint() {
        HttpHeaders headers = mock(HttpHeaders.class);
        MultivaluedMap<String, String> headerValues = new MultivaluedHashMap<>() {
            {
                put(HttpHeaders.AUTHORIZATION, List.of("BEARER 1234"));
            }
        };

        RealmModel masterRealm = mock(RealmModel.class);
        RealmModel anotherRealm = mock(RealmModel.class);
        RealmProvider realmProvider = mock(RealmProvider.class);
        UserProvider userProvider = mock(UserProvider.class);
        Stream<UserModel> usersMaster = Arrays
                .stream(new UserModel[] { UserModelHelper.buildUser("1"), UserModelHelper.buildUser("2") });
        when(userProvider.searchForUserStream(masterRealm, Map.of(UserModel.SEARCH, "*"))).thenReturn(usersMaster);
        Stream<UserModel> usersOther = Arrays
                .stream(new UserModel[] { UserModelHelper.buildUser("1"), UserModelHelper.buildUser("2") });
        when(userProvider.searchForUserStream(anotherRealm, Map.of(UserModel.SEARCH, "*"))).thenReturn(usersOther);
        when(realmProvider.getRealmsStream()).thenReturn(Arrays.stream(new RealmModel[] { masterRealm, anotherRealm }));
        when(realmProvider.getRealmByName("master")).thenReturn(masterRealm);
        when(realmProvider.getRealmByName("other")).thenReturn(anotherRealm);
        when(headers.getRequestHeaders()).thenReturn(headerValues);
        KeycloakContext context = mock(KeycloakContext.class);
        when(context.getRealm()).thenReturn(masterRealm);
        when(context.getRequestHeaders()).thenReturn(headers);
        when(session.getContext()).thenReturn(context);
        when(session.realms()).thenReturn(realmProvider);
        when(session.users()).thenReturn(userProvider);
        try (MockedStatic<Tokens> tokenMock = mockStatic(Tokens.class)) {
            AccessToken token = new AccessToken();
            token.issuer("master");
            token.setRealmAccess(new AccessToken.Access());
            tokenMock.when(() -> Tokens.getAccessToken(session)).thenReturn(token);
            AuditEndpoint endpoint = new AuditEndpoint(session) {
                @Override
                public void authenticate() {
                }
            };
            return endpoint.listUsers(headers);
        }
    }

    protected static List<AuditedClientRepresentation> getClientsViaEndpoint() {
        HttpHeaders headers = mock(HttpHeaders.class);
        MultivaluedMap<String, String> headerValues = new MultivaluedHashMap<>() {
            {
                put(HttpHeaders.AUTHORIZATION, List.of("BEARER 1234"));
            }
        };

        RealmModel masterRealm = mock(RealmModel.class);
        RealmModel anotherRealm = mock(RealmModel.class);
        RealmProvider realmProvider = mock(RealmProvider.class);
        ClientProvider clientProvider = mock(ClientProvider.class);
        Stream<ClientModel> clientsMaster = Arrays
                .stream(new ClientModel[] { ClientModelHelper.buildClient(), ClientModelHelper.buildClient() });
        when(clientProvider.getClientsStream(masterRealm)).thenReturn(clientsMaster);
        Stream<ClientModel> clientsOther = Arrays
                .stream(new ClientModel[] { ClientModelHelper.buildClient(), ClientModelHelper.buildClient() });
        when(clientProvider.getClientsStream(anotherRealm)).thenReturn(clientsOther);
        when(realmProvider.getRealmsStream()).thenReturn(Arrays.stream(new RealmModel[] { masterRealm, anotherRealm }));
        when(realmProvider.getRealmByName("master")).thenReturn(masterRealm);
        when(realmProvider.getRealmByName("other")).thenReturn(anotherRealm);
        when(headers.getRequestHeaders()).thenReturn(headerValues);
        KeycloakContext context = mock(KeycloakContext.class);
        when(context.getRealm()).thenReturn(masterRealm);
        when(context.getRequestHeaders()).thenReturn(headers);
        when(session.getContext()).thenReturn(context);
        when(session.realms()).thenReturn(realmProvider);
        when(session.clients()).thenReturn(clientProvider);
        try (MockedStatic<Tokens> tokenMock = mockStatic(Tokens.class)) {
            AccessToken token = new AccessToken();
            token.issuer("master");
            token.setRealmAccess(new AccessToken.Access());
            tokenMock.when(() -> Tokens.getAccessToken(session)).thenReturn(token);
            AuditEndpoint endpoint = new AuditEndpoint(session) {
                @Override
                public void authenticate() {
                }
            };
            return endpoint.listClients(headers);
        }
    }
}
