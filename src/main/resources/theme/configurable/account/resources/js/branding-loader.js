// Applies realm branding to the React based account/admin consoles. Loaded as an ES module by the
// console's index.ftl, so the script location comes from import.meta.url.
const script = new URL(import.meta.url);
const contextPath = script.pathname.slice(0, script.pathname.indexOf("/resources/"));
const route = location.pathname.slice(contextPath.length);
const match = route.match(/^\/(realms|admin)\/([^/]+)/);
const isAdmin = match?.[1] === "admin";

function addStylesheet(href) {
  const link = document.createElement("link");
  link.rel = "stylesheet";
  link.href = href;
  document.head.appendChild(link);
}

function setFavicon(href, type) {
  document.querySelectorAll("link[rel~='icon']").forEach((el) => el.remove());
  const link = document.createElement("link");
  link.rel = "icon";
  link.type = type;
  link.href = href;
  document.head.appendChild(link);
}

function enforceColorScheme(scheme) {
  if (scheme !== "light" && scheme !== "dark") {
    return;
  }
  const apply = () => {
    document.documentElement.classList.toggle("pf-v5-theme-dark", scheme === "dark");
  };
  apply();
  new MutationObserver(apply).observe(document.documentElement, { attributes: true, attributeFilter: ["class"] });
}

if (match) {
  const realm = decodeURIComponent(match[2]);
  fetch(`${contextPath}/realms/${encodeURIComponent(realm)}/branding/config.json`, { credentials: "omit" })
    .then((response) => (response.ok ? response.json() : null))
    .then((cfg) => {
      if (!cfg || !cfg.enabled || (isAdmin ? !cfg.applyToAdmin : !cfg.applyToAccount)) {
        return;
      }
      document.documentElement.classList.add("cfg-console");
      if (cfg.fontCssUrl) {
        addStylesheet(cfg.fontCssUrl);
      }
      addStylesheet(cfg.cssUrl);
      if (cfg.faviconUrl) {
        setFavicon(cfg.faviconUrl, cfg.faviconType);
      }
      enforceColorScheme(cfg.colorScheme);
    })
    .catch((error) => console.warn("Branding could not be applied", error));
}
