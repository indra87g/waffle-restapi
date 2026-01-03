package com.indra87g.api;

import cn.nukkit.Server;
import cn.nukkit.utils.Config;
import com.indra87g.api.handler.ServerInfo;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

public class ApiService {

    private HttpServer server;
    private final Server nukkitServer;
    private final Config config;
    private final ServerInfo serverInfo;
    private final int port;

    public ApiService(Server nukkitServer, Config config, int port, int updateInterval) {
        this.nukkitServer = nukkitServer;
        this.config = config;
        this.port = port;
        this.serverInfo = new ServerInfo(nukkitServer, updateInterval);
    }

    public void start() throws IOException {
        URI baseUri = UriBuilder.fromUri("http://0.0.0.0/").port(port).build();
        ResourceConfig resourceConfig = new ResourceConfig()
                .packages("com.indra87g.api.handler")
                .register(new ApiKeyFilter(config))
                .register(MultiPartFeature.class)
                .register(new Binder(nukkitServer, config, serverInfo));

        server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig);
    }

    public void stop() {
        if (server != null) {
            server.shutdownNow();
        }
        serverInfo.shutdownScheduler();
    }
}