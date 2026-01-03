package com.indra87g.api.handler;

import cn.nukkit.Server;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Path("/")
public class RootHandler {

    @Context
    private Server nukkitServer;

    @GET
    public Response handle() throws IOException {
        File indexFile = new File(nukkitServer.getPluginPath() + "index.html");
        String response;
        String contentType;
        if (indexFile.exists()) {
            response = new String(Files.readAllBytes(indexFile.toPath()));
            contentType = "text/html";
        } else {
            response = "<h1>Hello World!</h1>";
            contentType = "text/html";
        }
        return Response.ok(response).header("Content-Type", contentType).build();
    }
}