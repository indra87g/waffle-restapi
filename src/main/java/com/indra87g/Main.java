package com.indra87g;

import com.indra87g.api.ApiService;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;

import java.io.IOException;

public class Main extends PluginBase {

    private ApiService apiService;

    @Override
    public void onEnable() {
        getLogger().info("plugin activated!");
        saveDefaultConfig();
        Config config = getConfig();
        int port = config.getInt("port", 8080);
        int updateInterval = config.getInt("update-interval-seconds", 10);

        apiService = new ApiService(this.getServer(), config, port, updateInterval);
        try {
            apiService.start();
            getLogger().info("REST API server started on port " + port);
        } catch (IOException e) {
            getLogger().error("Failed to start REST API server", e);
        }
    }

    @Override
    public void onDisable() {
        if (apiService != null) {
            apiService.stop();
            getLogger().info("REST API server stopped");
        }
    }
}
