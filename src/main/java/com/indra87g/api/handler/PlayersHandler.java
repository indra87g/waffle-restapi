package com.indra87g.api.handler;

import cn.nukkit.Player;
import cn.nukkit.Server;
import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/players")
public class PlayersHandler {

    @Context
    private Server nukkitServer;
    private final Gson gson = new Gson();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response handle() {
        List<PlayerInfo> playerList = new ArrayList<>();
        List<PlayerInfo> opList = new ArrayList<>();

        for (Player player : nukkitServer.getOnlinePlayers().values()) {
            PlayerInfo playerInfo = new PlayerInfo(player.getUniqueId().getMostSignificantBits(), player.getName());
            if (player.isOp()) {
                opList.add(playerInfo);
            } else {
                playerList.add(playerInfo);
            }
        }
        String json = gson.toJson(new PlayersInfo(playerList, opList));
        return Response.ok(json).build();
    }

    @Getter
    @AllArgsConstructor
    private static class PlayerInfo {
        private final long uuid;
        private final String name;
    }

    @Getter
    @AllArgsConstructor
    private static class PlayersInfo {
        private final List<PlayerInfo> players;
        private final List<PlayerInfo> op;
    }
}