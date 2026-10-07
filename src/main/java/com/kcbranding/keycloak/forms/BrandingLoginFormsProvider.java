package com.kcbranding.keycloak.forms;

import java.util.Locale;

import com.kcbranding.keycloak.theme.BrandingBean;
import jakarta.ws.rs.core.Response;
import org.keycloak.forms.login.freemarker.FreeMarkerLoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.theme.Theme;

public class BrandingLoginFormsProvider extends FreeMarkerLoginFormsProvider {

    public BrandingLoginFormsProvider(KeycloakSession session) {
        super(session);
    }

    @Override
    protected Response processTemplate(Theme theme, String templateName, Locale locale) {
        // processTemplate is the single sink for every login page, error page and info page.
        RealmModel target = realm != null ? realm : session.getContext().getRealm();
        BrandingBean bean = BrandingBean.create(session, target);
        if (bean != null) {
            attributes.put(BrandingBean.ATTRIBUTE, bean);
        }
        return super.processTemplate(theme, templateName, locale);
    }
}
