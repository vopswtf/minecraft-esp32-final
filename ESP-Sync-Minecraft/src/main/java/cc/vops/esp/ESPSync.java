package cc.vops.esp;

import cc.vops.esp.listener.StatusListener;
import cc.vops.esp.socket.ESPSocketServer;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class ESPSync extends JavaPlugin {
    @Getter private static ESPSync instance;
    @Getter private ESPSocketServer websocketServer;

    @Override
    public void onEnable() {
        instance = this;

        try {
            websocketServer = new ESPSocketServer(8080);
            websocketServer.start();
        } catch (Exception e) {
            this.getLogger().severe("Failed to start WebSocket server");
            e.printStackTrace();
        }

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (websocketServer == null) return;
            websocketServer.tick();
        }, 0, 5L);

        Bukkit.getPluginManager().registerEvents(new StatusListener(), this);
    }

    @Override
    public void onDisable() {
        if (websocketServer != null) {
            try {
                websocketServer.stop();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
