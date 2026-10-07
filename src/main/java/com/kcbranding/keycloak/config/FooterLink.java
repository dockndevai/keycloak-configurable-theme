package com.kcbranding.keycloak.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A footer link parsed from the {@code footerLinks} setting ({@code Label|url;Label|url}).
 * A plain bean rather than a record: FreeMarker would resolve {@code link.url} to the record accessor
 * method instead of its value.
 */
public final class FooterLink {

    private final String label;
    private final String url;

    public FooterLink(String label, String url) {
        this.label = label;
        this.url = url;
    }

    /** Parses without validating; entries without a separator get an empty URL. */
    public static List<FooterLink> parseLenient(String value) {
        List<FooterLink> links = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return links;
        }
        for (String entry : value.split("[;\\n]")) {
            if (entry.isBlank()) {
                continue;
            }
            int sep = entry.indexOf('|');
            if (sep < 0) {
                links.add(new FooterLink(entry.strip(), ""));
            } else {
                links.add(new FooterLink(entry.substring(0, sep).strip(), entry.substring(sep + 1).strip()));
            }
        }
        return links;
    }

    public String getLabel() {
        return label;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FooterLink other && label.equals(other.label) && url.equals(other.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, url);
    }

    @Override
    public String toString() {
        return label + "|" + url;
    }
}
