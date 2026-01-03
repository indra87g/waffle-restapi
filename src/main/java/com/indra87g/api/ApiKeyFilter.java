package com.indra87g.api;

import cn.nukkit.utils.Config;
import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.List;

@Provider
public class ApiKeyFilter implements ContainerRequestFilter {

    private final Config config;
    private final Gson gson = new Gson();

    public ApiKeyFilter(Config config) {
        this.config = config;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (config.getBoolean("api-key.enabled", false)) {
            String path = "/" + requestContext.getUriInfo().getPath();
            List<String> protectedEndpoints = config.getStringList("api-key.protected-endpoints");

            if (protectedEndpoints.contains(path)) {
                String apiKey = requestContext.getHeaderString("X-API-KEY");
                String configuredApiKey = config.getString("api-key.key");

                if (apiKey == null || !apiKey.equals(configuredApiKey)) {
                    requestContext.abortWith(
                            Response.status(Response.Status.UNAUTHORIZED)
                                    .entity(gson.toJson(new ErrorResponse("Unauthorized: Invalid or missing API key")))
                                    .build()
                    );
                }
            }
        }
    }

    @Getter
    @AllArgsConstructor
    private static class ErrorResponse {
        private final String error;
    }
}