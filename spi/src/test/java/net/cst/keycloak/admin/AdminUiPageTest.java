package net.cst.keycloak.admin;

import net.cst.keycloak.utils.EndpointTest;
import org.junit.jupiter.api.Test;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class AdminUiPageTest extends EndpointTest {

    private ComponentModel componentModel = mock(ComponentModel.class);
    private RealmModel masterRealm = mock(RealmModel.class);
    private Config.Scope config = mock(Config.Scope.class);
    private KeycloakSessionFactory sessionFactory = mock(KeycloakSessionFactory.class);

    @Test
    void shouldInit() {
        AdminUiPage page = new AdminUiPage();
        page.onCreate(session, masterRealm, componentModel);
        page.init(config);
        page.postInit(sessionFactory);
        page.close();
        assertNotNull(page.getConfigProperties());
        assertNotNull(page.getId());
        assertNotNull(page.getHelpText());
        assertNotNull(page.getTypeMetadata());
    }

}
