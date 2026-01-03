package com.indra87g.api.handler;

import com.google.gson.Gson;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/server")
public class ServerHandler {

    @Context
    private ServerInfo serverInfo;
    private final Gson gson = new Gson();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response handle() {
        String json = gson.toJson(serverInfo);
        return Response.ok(json).build();
    }
}