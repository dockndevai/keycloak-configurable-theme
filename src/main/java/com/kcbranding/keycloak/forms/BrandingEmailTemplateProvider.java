package com.kcbranding.keycloak.forms;

import java.util.List;
import java.util.Map;

import com.kcbranding.keycloak.theme.BrandingBean;
import org.keycloak.email.EmailException;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.models.KeycloakSession;

public class BrandingEmailTemplateProvider extends FreeMarkerEmailTemplateProvider {

    public BrandingEmailTemplateProvider(KeycloakSession session) {
        super(session);
    }

    @Override
    protected EmailTemplate processTemplate(String subjectKey, List<Object> subjectAttributes, String template,
                                            Map<String, Object> attributes) throws EmailException {
        BrandingBean bean = BrandingBean.create(session, realm != null ? realm : session.getContext().getRealm());
        if (bean != null) {
            attributes.put(BrandingBean.ATTRIBUTE, bean);
        }
        return super.processTemplate(subjectKey, subjectAttributes, template, attributes);
    }
}
