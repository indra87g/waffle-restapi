package com.indra87g.api;

import cn.nukkit.Server;
import cn.nukkit.utils.Config;
import com.indra87g.api.handler.ServerInfo;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

public class Binder extends AbstractBinder {

    private final Server nukkitServer;
    private final Config config;
    private final ServerInfo serverInfo;

    public Binder(Server nukkitServer, Config config, ServerInfo serverInfo) {
        this.nukkitServer = nukkitServer;
        this.config = config;
        this.serverInfo = serverInfo;
    }

    @Override
    protected void configure() {
        bind(nukkitServer).to(Server.class);
        bind(config).to(Config.class);
        bind(serverInfo).to(ServerInfo.class);
    }
}