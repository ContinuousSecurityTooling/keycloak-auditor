package net.cst.keycloak.admin;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.ui.extend.UiPageProvider;
import org.keycloak.services.ui.extend.UiPageProviderFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Implements UiPageProvider so it will be a master detail view in the admin ui of TODO items
 */
@Slf4j
public class AdminUiPage implements UiPageProvider, UiPageProviderFactory<ComponentModel> {

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "auditing";
    }

    @Override
    public String getHelpText() {
        return "Here you view audit events from Keycloak";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("name")
                .label("Name")
                .helpText("Short name of the task")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add().property()
                .name("description")
                .label("Description")
                .helpText("Description of what needs to be done")
                .type(ProviderConfigProperty.TEXT_TYPE)
                .add().property()
                .name("prio")
                .label("Priority")
                .type(ProviderConfigProperty.LIST_TYPE)
                .options("critical", "high priority", "neutral", "low priority", "unknown")
                .add().build();
    }

    @Override
    public Map<String, Object> getTypeMetadata() {
        Map<String, Object> metaData = new HashMap<>();
        metaData.put("displayFields", List.of("name", "prio"));
        return metaData;
    }
}
