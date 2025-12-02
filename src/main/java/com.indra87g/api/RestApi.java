package com.indra87g.api;

import cn.nukkit.Server;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class RestApi {

    private HttpServer server;
    private final Server nukkitServer;

    public RestApi(Server nukkitServer) {
        this.nukkitServer = nukkitServer;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(8080), 0);
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
            // Manually construct the JSON
            StringBuilder json = new StringBuilder();
            json.append("[");
            boolean first = true;
            for (cn.nukkit.Player player : nukkitServer.getOnlinePlayers().values()) {
                if (!first) {
                    json.append(",");
                }
                json.append("{\"name\":\"").append(player.getName()).append("\"}");
                first = false;
            }
            json.append("]");

            String response = json.toString();
            t.getResponseHeaders().set("Content-Type", "application/json");
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    class ServerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            // Manually construct the JSON
            long totalMemory = Runtime.getRuntime().totalMemory();
            long freeMemory = Runtime.getRuntime().freeMemory();
            long usedMemory = totalMemory - freeMemory;

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"ram\":{");
            json.append("\"total\":").append(totalMemory).append(",");
            json.append("\"free\":").append(freeMemory).append(",");
            json.append("\"used\":").append(usedMemory);
            json.append("},");
            // CPU and storage are more complex to get without external libraries,
            // so for now I will just return ram usage.
            json.append("\"players\":").append(nukkitServer.getOnlinePlayers().size());
            json.append("}");

            String response = json.toString();
            t.getResponseHeaders().set("Content-Type", "application/json");
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
