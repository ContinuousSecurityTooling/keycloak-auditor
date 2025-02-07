package net.cst.keycloak.utils;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import net.cst.keycloak.audit.model.AuditedClientRepresentation;
import net.cst.keycloak.audit.model.AuditedUserRepresentation;
import net.cst.keycloak.audit.model.ConfigConstants;
import net.cst.keycloak.events.logging.util.ClientModelHelper;
import net.cst.keycloak.events.logging.util.UserModelHelper;
import net.cst.keycloak.resources.AuditEndpoint;
import org.junit.jupiter.api.BeforeEach;
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

import static org.mockito.Mockito.*;

public abstract class EndpointTest {

    protected static KeycloakSession session;
    protected static AccessToken auth;

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
                .stream(new UserModel[]{UserModelHelper.buildUser("1"), UserModelHelper.buildUser("2")});
        when(userProvider.searchForUserStream(masterRealm, Map.of(UserModel.SEARCH, "*"))).thenReturn(usersMaster);
        Stream<UserModel> usersOther = Arrays
                .stream(new UserModel[]{UserModelHelper.buildUser("1"), UserModelHelper.buildUser("2")});
        when(userProvider.searchForUserStream(anotherRealm, Map.of(UserModel.SEARCH, "*"))).thenReturn(usersOther);
        when(realmProvider.getRealmsStream()).thenReturn(Arrays.stream(new RealmModel[]{masterRealm, anotherRealm}));
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
            return mockAccessToken(tokenMock).listUsers(headers);
        }
    }

    private static AuditEndpoint mockAccessToken(MockedStatic<Tokens> tokenMock) {
        auth = new AccessToken();
        auth.issuer("master");
        auth.setRealmAccess(new AccessToken.Access().addRole(ConfigHelper.getConfigValue(ConfigConstants.DEFAULT_ROLE)));
        tokenMock.when(() -> Tokens.getAccessToken(session)).thenReturn(auth);
        return new AuditEndpoint(session) {
            @Override
            public void authenticate() {
            }
        };
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
                .stream(new ClientModel[]{ClientModelHelper.buildClient(), ClientModelHelper.buildClient()});
        when(clientProvider.getClientsStream(masterRealm)).thenReturn(clientsMaster);
        Stream<ClientModel> clientsOther = Arrays
                .stream(new ClientModel[]{ClientModelHelper.buildClient(), ClientModelHelper.buildClient()});
        when(clientProvider.getClientsStream(anotherRealm)).thenReturn(clientsOther);
        when(realmProvider.getRealmsStream()).thenReturn(Arrays.stream(new RealmModel[]{masterRealm, anotherRealm}));
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
            return mockAccessToken(tokenMock).listClients(headers);
        }
    }

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
}
