<#-- Branded wrapper for all HTML emails. "branding" is provided by BrandingEmailTemplateProvider. -->
<#macro emailLayout>
<html lang="${locale.language}" dir="${(ltr)?then('ltr','rtl')}">
<#if branding??>
<body style="margin:0;padding:0;background-color:${branding.backgroundColor};font-family:${branding.fontFamily};">
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background-color:${branding.backgroundColor};">
  <tr>
    <td align="center" style="padding:32px 16px;">
      <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0"
             style="max-width:600px;background-color:${branding.cardBackgroundColor};border-radius:8px;border-top:4px solid ${branding.primaryColor};">
        <tr>
          <td align="center" style="padding:28px 32px 8px;">
            <#if branding.logoUrl?has_content>
              <img src="${branding.logoUrl}" alt="${realmName}" style="height:${branding.logoHeight};max-width:100%;border:0;" />
            <#else>
              <span style="font-size:22px;font-weight:bold;color:${branding.primaryColor};">${realmName}</span>
            </#if>
          </td>
        </tr>
        <tr>
          <td style="padding:16px 32px 32px;color:${branding.textColor};font-size:15px;line-height:1.6;">
            <#nested>
          </td>
        </tr>
      </table>
      <#if branding.footerText?has_content>
      <p style="max-width:600px;margin:16px auto 0;color:#6b7280;font-size:12px;text-align:center;">${branding.footerText}</p>
      </#if>
    </td>
  </tr>
</table>
</body>
<#else>
<body>
    <#nested>
</body>
</#if>
</html>
</#macro>
