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
 * Adds an "Auditing" entry to the admin console sidebar that links to the
 * audit report download page served by {@code AuditEndpoint}.
 */
@Slf4j
public class AdminUiPage implements UiPageProvider, UiPageProviderFactory<ComponentModel> {

    /** REST path of the audit resource provider (must match AuditedResourcesProviderFactory.CONTEXT_PATH). */
    private static final String AUDIT_BASE_PATH = "auditing";

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
        return AUDIT_BASE_PATH;
    }

    @Override
    public String getHelpText() {
        return "Download last-login audit reports for users and clients. "
                + "Navigate to <realm-url>/" + AUDIT_BASE_PATH + "/download for the interactive report page.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("reportScope")
                .label("Report Scope")
                .helpText("Whether to include all realms (requires global-master-access) or only the current realm")
                .type(ProviderConfigProperty.LIST_TYPE)
                .options("current-realm", "all-realms")
                .defaultValue("current-realm")
                .add()
                .build();
    }

    @Override
    public Map<String, Object> getTypeMetadata() {
        Map<String, Object> metaData = new HashMap<>();
        metaData.put("displayName", "Auditing");
        metaData.put("displayFields", List.of("reportScope"));
        metaData.put("downloadPage", AUDIT_BASE_PATH + "/download");
        metaData.put("usersEndpoint", AUDIT_BASE_PATH + "/users");
        metaData.put("usersCsvEndpoint", AUDIT_BASE_PATH + "/users/csv");
        metaData.put("clientsEndpoint", AUDIT_BASE_PATH + "/clients");
        metaData.put("clientsCsvEndpoint", AUDIT_BASE_PATH + "/clients/csv");
        return metaData;
    }
}
