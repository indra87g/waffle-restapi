package com.indra87g.api;

import cn.nukkit.Server;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiService {

    private HttpServer server;
    private final Server nukkitServer;
    private final int port;
    private final Gson gson = new Gson();

    public ApiService(Server nukkitServer, int port) {
        this.nukkitServer = nukkitServer;
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new MyHandler());
        server.createContext("/players", new PlayersHandler());
        server.createContext("/server", new ServerHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void sendJsonResponse(HttpExchange t, Object data) throws IOException {
        String response = gson.toJson(data);
        t.getResponseHeaders().set("Content-Type", "application/json");
        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "<h1>Hello World!</h1>";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    class PlayersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            List<PlayerInfo> playerList = new ArrayList<>();
            for (cn.nukkit.Player player : nukkitServer.getOnlinePlayers().values()) {
                playerList.add(new PlayerInfo(player.getName()));
            }
            sendJsonResponse(t, playerList);
        }
    }

    class ServerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            long totalMemory = Runtime.getRuntime().totalMemory();
            long freeMemory = Runtime.getRuntime().freeMemory();
            long usedMemory = totalMemory - freeMemory;

            RamInfo ramInfo = new RamInfo(totalMemory, freeMemory, usedMemory);
            ServerInfo serverInfo = new ServerInfo(ramInfo, nukkitServer.getOnlinePlayers().size());
            sendJsonResponse(t, serverInfo);
        }
    }

    // Simple data classes for JSON serialization
    private static class PlayerInfo {
        private final String name;

        public PlayerInfo(String name) {
            this.name = name;
        }
    }

    private static class RamInfo {
        private final long total;
        private final long free;
        private final long used;

        public RamInfo(long total, long free, long used) {
            this.total = total;
            this.free = free;
            this.used = used;
        }
    }

    private static class ServerInfo {
        private final RamInfo ram;
        private final int players;

        public ServerInfo(RamInfo ram, int players) {
            this.ram = ram;
            this.players = players;
        }
    }
}
