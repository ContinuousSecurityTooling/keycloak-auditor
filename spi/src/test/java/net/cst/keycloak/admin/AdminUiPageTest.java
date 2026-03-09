package net.cst.keycloak.admin;

import net.cst.keycloak.utils.EndpointTest;
import org.junit.jupiter.api.Test;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AdminUiPageTest extends EndpointTest {

    private final ComponentModel componentModel = mock(ComponentModel.class);
    private final RealmModel masterRealm = mock(RealmModel.class);
    private final Config.Scope config = mock(Config.Scope.class);
    private final KeycloakSessionFactory sessionFactory = mock(KeycloakSessionFactory.class);

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

    @Test
    void idShouldMatchAuditContextPath() {
        assertEquals("auditing", new AdminUiPage().getId());
    }

    @Test
    void typeMetadataShouldExposeAllDownloadEndpoints() {
        Map<String, Object> meta = new AdminUiPage().getTypeMetadata();

        assertTrue(meta.containsKey("downloadPage"), "downloadPage key missing");
        assertTrue(meta.containsKey("usersEndpoint"), "usersEndpoint key missing");
        assertTrue(meta.containsKey("usersCsvEndpoint"), "usersCsvEndpoint key missing");
        assertTrue(meta.containsKey("clientsEndpoint"), "clientsEndpoint key missing");
        assertTrue(meta.containsKey("clientsCsvEndpoint"), "clientsCsvEndpoint key missing");

        assertTrue(meta.get("downloadPage").toString().endsWith("/download"));
        assertTrue(meta.get("usersCsvEndpoint").toString().endsWith("/csv"));
        assertTrue(meta.get("clientsCsvEndpoint").toString().endsWith("/csv"));
    }

    @Test
    void typeMetadataShouldExposeDisplayNameAuditing() {
        Map<String, Object> meta = new AdminUiPage().getTypeMetadata();
        assertEquals("Auditing", meta.get("displayName"), "displayName should be 'Auditing'");
    }

    @Test
    void configPropertiesShouldContainReportScopeProperty() {
        boolean hasReportScope = new AdminUiPage().getConfigProperties().stream()
                .anyMatch(p -> "reportScope".equals(p.getName()));
        assertTrue(hasReportScope, "reportScope config property missing");
    }

    @Test
    void displayFieldsShouldReferenceReportScope() {
        Object displayFields = new AdminUiPage().getTypeMetadata().get("displayFields");
        assertNotNull(displayFields);
        assertTrue(displayFields instanceof List<?>);
        assertTrue(((List<?>) displayFields).contains("reportScope"));
    }
}
