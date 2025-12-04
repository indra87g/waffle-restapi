package com.indra87g.api;

import cn.nukkit.Server;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ApiService {

    private HttpServer server;
    private final Server nukkitServer;
    private final int port;
    private final Gson gson = new Gson();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ServerInfo serverInfo;

    public ApiService(Server nukkitServer, int port, int updateInterval) {
        this.nukkitServer = nukkitServer;
        this.port = port;
        scheduler.scheduleAtFixedRate(this::updateServerInfo, 0, updateInterval, TimeUnit.SECONDS);
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
        scheduler.shutdown();
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
            File indexFile = new File(nukkitServer.getPluginPath() + "/ContohPlugin/index.html");
            String response;
            if (indexFile.exists()) {
                response = new String(Files.readAllBytes(Paths.get(indexFile.toURI())));
                t.getResponseHeaders().set("Content-Type", "text/html");
            } else {
                response = "<h1>Hello World!</h1>";
                t.getResponseHeaders().set("Content-Type", "text/html");
            }
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
            List<PlayerInfo> opList = new ArrayList<>();

            for (cn.nukkit.Player player : nukkitServer.getOnlinePlayers().values()) {
                PlayerInfo playerInfo = new PlayerInfo(player.getUniqueId().getMostSignificantBits(), player.getName());
                if (player.isOp()) {
                    opList.add(playerInfo);
                } else {
                    playerList.add(playerInfo);
                }
            }
            sendJsonResponse(t, new PlayersInfo(playerList, opList));
        }
    }

    private void updateServerInfo() {
        // RAM Info
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;
        RamInfo ramInfo = new RamInfo(
                bytesToMegabytes(totalMemory),
                bytesToMegabytes(freeMemory),
                bytesToMegabytes(usedMemory)
        );

        // CPU Info
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double processCpuLoad = osBean.getSystemLoadAverage();
        double systemCpuLoad = osBean.getSystemLoadAverage();
        int availableProcessors = osBean.getAvailableProcessors();
        CpuInfo cpuInfo = new CpuInfo(processCpuLoad, systemCpuLoad, availableProcessors);

        // Storage Info
        File disk = new File("/");
        long totalSpace = disk.getTotalSpace();
        long freeSpace = disk.getFreeSpace();
        long usedSpace = totalSpace - freeSpace;
        StorageInfo storageInfo = new StorageInfo(
                bytesToMegabytes(totalSpace),
                bytesToMegabytes(freeSpace),
                bytesToMegabytes(usedSpace)
        );

        // Server Time
        String serverTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        this.serverInfo = new ServerInfo(
                ramInfo,
                cpuInfo,
                storageInfo,
                nukkitServer.getOnlinePlayers().size(),
                nukkitServer.getMotd(),
                serverTime
        );
    }

    private String bytesToMegabytes(long bytes) {
        return bytes / (1024 * 1024) + "MB";
    }

    class ServerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            sendJsonResponse(t, serverInfo);
        }
    }

    // Simple data classes for JSON serialization
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

    @Getter
    @AllArgsConstructor
    private static class RamInfo {
        private final String total;
        private final String free;
        private final String used;
    }

    @Getter
    @AllArgsConstructor
    private static class CpuInfo {
        private final double processCpuLoad;
        private final double systemCpuLoad;
        private final int availableProcessors;
    }

    @Getter
    @AllArgsConstructor
    private static class StorageInfo {
        private final String total;
        private final String free;
        private final String used;
    }

    @Getter
    @AllArgsConstructor
    private static class ServerInfo {
        private final RamInfo ram;
        private final CpuInfo cpu;
        private final StorageInfo storage;
        private final int players;
        private final String motd;
        private final String serverTime;
    }
}
