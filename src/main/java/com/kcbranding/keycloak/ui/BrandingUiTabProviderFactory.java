package com.kcbranding.keycloak.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.kcbranding.keycloak.config.BrandingField;
import com.kcbranding.keycloak.config.BrandingValidator;
import com.kcbranding.keycloak.spi.BrandingProvider;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.ui.extend.UiTabProvider;
import org.keycloak.services.ui.extend.UiTabProviderFactory;

/**
 * Adds a "Branding" tab to Realm settings in the admin console (requires the experimental
 * {@code declarative-ui} feature). Saving the tab writes the realm overrides used by
 * {@link BrandingProvider}; the REST API mirrors its changes back into this component.
 *
 * <p>Every field starts empty, meaning "inherit" from the global file / defaults, so saving the form
 * never pins values the admin did not touch.
 */
public class BrandingUiTabProviderFactory implements UiTabProviderFactory<ComponentModel> {

    public static final String ID = "Branding";
    static final String INHERIT = "inherit";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getPath() {
        return "/:realm/realm-settings/:tab";
    }

    @Override
    public Map<String, String> getParams() {
        return Map.of("tab", "branding");
    }

    @Override
    public String getHelpText() {
        return "Branding of the configurable theme for this realm. Empty fields inherit from the global "
                + "branding file or the built-in defaults.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> properties = new ArrayList<>();
        for (BrandingField field : BrandingField.values()) {
            ProviderConfigProperty p = new ProviderConfigProperty();
            p.setName(field.key());
            p.setLabel(field.label());
            String defaultHint = field.defaultValue().isEmpty() ? "" : " Default: " + field.defaultValue() + ".";
            p.setHelpText((field.help() == null ? "" : field.help()) + defaultHint);
            switch (field.type()) {
                case BOOLEAN -> {
                    p.setType(ProviderConfigProperty.LIST_TYPE);
                    p.setOptions(List.of(INHERIT, "true", "false"));
                    p.setDefaultValue(INHERIT);
                }
                case ENUM -> {
                    List<String> options = new ArrayList<>();
                    options.add(INHERIT);
                    options.addAll(field.options());
                    p.setType(ProviderConfigProperty.LIST_TYPE);
                    p.setOptions(options);
                    p.setDefaultValue(INHERIT);
                }
                case CSS, LINKS -> p.setType(ProviderConfigProperty.TEXT_TYPE);
                default -> p.setType(ProviderConfigProperty.STRING_TYPE);
            }
            properties.add(p);
        }
        return properties;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model)
            throws ComponentValidationException {
        BrandingValidator.Result result = BrandingValidator.validate(toOverrides(model));
        if (!result.isValid()) {
            throw new ComponentValidationException(String.join("; ", result.errors()));
        }
    }

    @Override
    public void onCreate(KeycloakSession session, RealmModel realm, ComponentModel model) {
        apply(session, realm, model);
    }

    @Override
    public void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel) {
        apply(session, realm, newModel);
    }

    private static void apply(KeycloakSession session, RealmModel realm, ComponentModel model) {
        BrandingProvider provider = session.getProvider(BrandingProvider.class);
        provider.setRealmOverrides(realm, BrandingValidator.validate(toOverrides(model)).values());
    }

    static Map<String, String> toOverrides(ComponentModel model) {
        Map<String, String> values = new LinkedHashMap<>();
        for (BrandingField field : BrandingField.values()) {
            String value = model.get(field.key());
            if (value != null && !value.isBlank() && !INHERIT.equals(value)) {
                values.put(field.key(), value);
            }
        }
        return values;
    }

    /** Mirrors overrides written through the REST API into the tab's component, if one exists. */
    public static void syncComponent(KeycloakSession session, RealmModel realm, Map<String, String> overrides) {
        realm.getComponentsStream(realm.getId(), UiTabProvider.class.getName())
                .filter(c -> ID.equals(c.getProviderId()))
                .toList()
                .forEach(component -> {
                    for (BrandingField field : BrandingField.values()) {
                        String value = overrides.get(field.key());
                        boolean choice = field.type() == BrandingField.Type.BOOLEAN || field.type() == BrandingField.Type.ENUM;
                        component.put(field.key(), value != null ? value : (choice ? INHERIT : ""));
                    }
                    realm.updateComponent(component);
                });
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
}
