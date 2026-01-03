package com.indra87g.api.handler;

import cn.nukkit.Server;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public class ServerInfo {
    private RamInfo ram;
    private CpuInfo cpu;
    private StorageInfo storage;
    private int players;
    private String motd;
    private String serverTime;

    private final Server nukkitServer;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public ServerInfo(Server nukkitServer, int updateInterval) {
        this.nukkitServer = nukkitServer;
        scheduler.scheduleAtFixedRate(this::updateServerInfo, 0, updateInterval, TimeUnit.SECONDS);
    }

    private void updateServerInfo() {
        // RAM Info
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;
        this.ram = new RamInfo(
                bytesToMegabytes(totalMemory),
                bytesToMegabytes(freeMemory),
                bytesToMegabytes(usedMemory)
        );

        // CPU Info
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double processCpuLoad = osBean.getSystemLoadAverage();
        double systemCpuLoad = osBean.getSystemLoadAverage();
        int availableProcessors = osBean.getAvailableProcessors();
        this.cpu = new CpuInfo(processCpuLoad, systemCpuLoad, availableProcessors);

        // Storage Info
        File disk = new File("/");
        long totalSpace = disk.getTotalSpace();
        long freeSpace = disk.getFreeSpace();
        long usedSpace = totalSpace - freeSpace;
        this.storage = new StorageInfo(
                bytesToMegabytes(totalSpace),
                bytesToMegabytes(freeSpace),
                bytesToMegabytes(usedSpace)
        );

        // Server Time
        this.serverTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        this.players = nukkitServer.getOnlinePlayers().size();
        this.motd = nukkitServer.getMotd();
    }

    private String bytesToMegabytes(long bytes) {
        return bytes / (1024 * 1024) + "MB";
    }

    public void shutdownScheduler() {
        scheduler.shutdown();
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
}