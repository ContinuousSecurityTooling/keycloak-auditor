package net.cst.keycloak.user;

import net.cst.keycloak.utils.EndpointTest;
import org.junit.jupiter.api.Test;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class AccountUiPageTest extends EndpointTest {

    private ComponentModel componentModel = mock(ComponentModel.class);
    private RealmModel masterRealm = mock(RealmModel.class);
    private Config.Scope config = mock(Config.Scope.class);
    private KeycloakSessionFactory sessionFactory = mock(KeycloakSessionFactory.class);

    @Test
    void shouldInit() {
        AccountUiPage page = new AccountUiPage();
        page.onCreate(session, masterRealm, componentModel);
        page.init(config);
        page.postInit(sessionFactory);
        assertNotNull(page.getConfigProperties());
        assertNotNull(page.getId());
        assertNotNull(page.getHelpText());
        assertNotNull(page.getPath());
    }

}
