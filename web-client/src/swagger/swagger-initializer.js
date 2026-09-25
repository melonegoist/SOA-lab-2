// "Try it out" sends real requests to the servers listed in the specifications.
// The server on the same host as this page (localhost through the tunnel, or
// helios.cs.ifmo.ru from the university network) is listed first, so it is
// the one selected. validatorUrl: null keeps the specifications from being
// sent to validator.swagger.io.
const ServerOfThisHostFirst = () => ({
    statePlugins: {
        spec: {
            wrapActions: {
                updateJsonSpec: original => spec => {
                    if (!Array.isArray(spec?.servers)) {
                        return original(spec);
                    }
                    const here = server => new URL(server.url, location.href).hostname === location.hostname;
                    const servers = [...spec.servers.filter(here), ...spec.servers.filter(server => !here(server))];
                    return original({ ...spec, servers });
                },
            },
        },
    },
});

window.ui = SwaggerUIBundle({
    urls: [
        { url: 'specs/vehicle-service.yaml', name: 'Vehicle service' },
        { url: 'specs/shop-service.yaml', name: 'Shop service' },
    ],
    dom_id: '#swagger-ui',
    deepLinking: true,
    presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
    plugins: [SwaggerUIBundle.plugins.DownloadUrl, ServerOfThisHostFirst],
    layout: 'StandaloneLayout',
    validatorUrl: null,
    tryItOutEnabled: true,
    displayRequestDuration: true,
});
