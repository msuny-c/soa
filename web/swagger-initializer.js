window.onload = function () {
  window.ui = SwaggerUIBundle({
    urls: [
      { url: "worker-service.yaml", name: "Worker Collection Service" },
      { url: "hr-service.yaml", name: "HR Service" }
    ],
    "urls.primaryName": "Worker Collection Service",
    dom_id: "#swagger-ui",
    deepLinking: true,
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl
    ],
    layout: "StandaloneLayout",
    tryItOutEnabled: false,
    defaultModelsExpandDepth: 1,
    docExpansion: "list"
  });
};
