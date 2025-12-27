package com.indra87g.api;

import cn.nukkit.Server;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ApiService {

    private HttpServer server;
    private final Server nukkitServer;
    private final int port;
    private final String dropFolder;
    private final int maxFileSize;
    private final Gson gson = new Gson();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ServerInfo serverInfo;

    public ApiService(Server nukkitServer, int port, int updateInterval, String dropFolder, int maxFileSize) {
        this.nukkitServer = nukkitServer;
        this.port = port;
        this.dropFolder = dropFolder;
        this.maxFileSize = maxFileSize;
        scheduler.scheduleAtFixedRate(this::updateServerInfo, 0, updateInterval, TimeUnit.SECONDS);
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new MyHandler());
        server.createContext("/players", new PlayersHandler());
        server.createContext("/server", new ServerHandler());
        server.createContext("/drop", new DropHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
        scheduler.shutdown();
    }

    private void sendJsonResponse(HttpExchange t, int statusCode, Object data) throws IOException {
        String response = gson.toJson(data);
        t.getResponseHeaders().set("Content-Type", "application/json");
        t.sendResponseHeaders(statusCode, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            File indexFile = new File(nukkitServer.getPluginPath() + "index.html");
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
            sendJsonResponse(t, 200, new PlayersInfo(playerList, opList));
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
            sendJsonResponse(t, 200, serverInfo);
        }
    }

    class DropHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (!"POST".equals(t.getRequestMethod())) {
                sendJsonResponse(t, 405, new ErrorResponse("Method Not Allowed"));
                return;
            }

            String contentType = t.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                sendJsonResponse(t, 400, new ErrorResponse("Bad Request: Content-Type must be multipart/form-data"));
                return;
            }

            try {
                Path dropFolderPath = Paths.get(dropFolder);
                if (!Files.exists(dropFolderPath)) {
                    Files.createDirectories(dropFolderPath);
                }

                String boundary = "--" + contentType.substring(contentType.indexOf("boundary=") + 9);
                byte[] boundaryBytes = boundary.getBytes();

                MultipartStreamReader msr = new MultipartStreamReader(t.getRequestBody(), boundaryBytes);

                if (!msr.skipToNextPart()) {
                     sendJsonResponse(t, 400, new ErrorResponse("Bad Request: Invalid multipart/form-data"));
                     return;
                }

                String headers = msr.readHeaders();
                String filename = getFilename(headers);

                if (filename == null || filename.isEmpty()) {
                    sendJsonResponse(t, 400, new ErrorResponse("Bad Request: Filename not found in multipart/form-data"));
                    return;
                }

                String sanitizedFilename = Paths.get(filename).getFileName().toString();
                Path filePath = dropFolderPath.resolve(sanitizedFilename);

                long maxFileSizeBytes = maxFileSize * 1024L * 1024L;

                try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                    long totalBytesWritten = msr.writeFile(fos, maxFileSizeBytes);

                    if (totalBytesWritten > maxFileSizeBytes) {
                        fos.close();
                        Files.delete(filePath);
                        sendJsonResponse(t, 413, new ErrorResponse("Request Entity Too Large. Max size is " + maxFileSize + "MB."));
                        return;
                    }
                }

                sendJsonResponse(t, 200, new SuccessResponse("File uploaded successfully to " + filePath));

            } catch (Exception e) {
                nukkitServer.getLogger().error("Failed to handle file upload", e);
                sendJsonResponse(t, 500, new ErrorResponse("Internal Server Error: " + e.getMessage()));
            }
        }

        private String getFilename(String headers) {
            String filename = null;
            if (headers != null) {
                for (String header : headers.split("\r\n")) {
                     if (header.toLowerCase().trim().startsWith("content-disposition:")) {
                        String[] parts = header.split(";");
                        for (String part : parts) {
                            part = part.trim();
                            if (part.startsWith("filename=")) {
                                filename = part.substring(part.indexOf("=") + 1).trim().replace("\"", "");
                            }
                        }
                     }
                }
            }
            return filename;
        }

    }

    class MultipartStreamReader {
        private final InputStream in;
        private final byte[] boundary;
        private final byte[] buffer;
        private int bufferLength = 0;
        private int bufferPos = 0;

        MultipartStreamReader(InputStream in, byte[] boundary) {
            this.in = in;
            this.boundary = boundary;
            this.buffer = new byte[8192];
        }

        boolean skipToNextPart() throws IOException {
            int boundaryIndex = -1;
            while(true) {
                fillBuffer();
                if(bufferLength == -1) return false;
                boundaryIndex = indexOf(buffer, bufferLength, boundary, 0);
                if(boundaryIndex != -1) {
                    bufferPos += boundaryIndex + boundary.length;
                    return true;
                }
                bufferPos = bufferLength;
            }
        }

        String readHeaders() throws IOException {
            ByteArrayOutputStream headerStream = new ByteArrayOutputStream();
            byte[] separator = {'\r', '\n', '\r', '\n'};
            int separatorIndex = -1;

            while(true) {
                fillBuffer();
                if(bufferLength == -1) return null;
                separatorIndex = indexOf(buffer, bufferLength, separator, 0);
                if (separatorIndex != -1) {
                    headerStream.write(buffer, bufferPos, separatorIndex - bufferPos);
                    bufferPos = separatorIndex + separator.length;
                    return headerStream.toString();
                }
                headerStream.write(buffer, bufferPos, bufferLength - bufferPos);
                bufferPos = bufferLength;
            }
        }

        long writeFile(OutputStream out, long maxSize) throws IOException {
            long totalBytesWritten = 0;
            int boundaryIndex = -1;

            while(true) {
                fillBuffer();
                if(bufferLength == -1) break;

                boundaryIndex = indexOf(buffer, bufferLength, boundary, 0);

                if (boundaryIndex != -1) {
                    int bytesToWrite = boundaryIndex - bufferPos;
                    if(bytesToWrite > 2) {
                        bytesToWrite -=2; // CRLF before boundary
                        totalBytesWritten += bytesToWrite;
                        if(totalBytesWritten > maxSize) return totalBytesWritten;
                        out.write(buffer, bufferPos, bytesToWrite);
                    }
                    bufferPos = boundaryIndex + boundary.length;
                    break;
                }

                totalBytesWritten += bufferLength - bufferPos;
                if(totalBytesWritten > maxSize) return totalBytesWritten;
                out.write(buffer, bufferPos, bufferLength - bufferPos);
                bufferPos = bufferLength;
            }
            return totalBytesWritten;
        }

        private void fillBuffer() throws IOException {
            if (bufferPos >= bufferLength) {
                bufferLength = in.read(buffer);
                bufferPos = 0;
            }
        }

        private int indexOf(byte[] src, int srcLength, byte[] target, int fromIndex) {
            for (int i = fromIndex; i <= srcLength - target.length; i++) {
                boolean found = true;
                for (int j = 0; j < target.length; j++) {
                    if (src[i + j] != target[j]) {
                        found = false;
                        break;
                    }
                }
                if (found) return i;
            }
            return -1;
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

    @Getter
    @AllArgsConstructor
    private static class ErrorResponse {
        private final String error;
    }

    @Getter
    @AllArgsConstructor
    private static class SuccessResponse {
        private final String message;
    }
}
