package net.cst.keycloak.user;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.ui.extend.UiTabProvider;
import org.keycloak.services.ui.extend.UiTabProviderFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountUiPage implements UiTabProvider, UiTabProviderFactory<ComponentModel> {

    private KeycloakSession session;

    @Override
    public String getId() {
        return "Auditing";
    }

    @Override
    public String getHelpText() {
        return "Show auditing information";
    }

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
    public void onCreate(KeycloakSession session, RealmModel realm, ComponentModel model) {
        realm.setAttribute("logo", model.get("logo"));
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        final ProviderConfigurationBuilder builder = ProviderConfigurationBuilder.create();
        builder.property()
                .name("logo")
                .label("Set a logo")
                .helpText("This logo will be shown on the account ui")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add();
        return builder.build();
    }

    @Override
    public String getPath() {
        return "/:realm/realm-settings/:tab?";
    }

    @Override
    public Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        params.put("tab", "attributes");
        return params;
    }
}
