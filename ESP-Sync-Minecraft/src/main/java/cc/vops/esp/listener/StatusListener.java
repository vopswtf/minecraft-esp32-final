package cc.vops.esp.listener;

import cc.vops.esp.ESPSync;
import cc.vops.esp.socket.ESPSocketServer;
import cc.vops.esp.util.PacketBuilder;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class StatusListener implements Listener {
    private String buildStatus(String message) {
        return new PacketBuilder("status").addField("message", message).build();
    }

    // this triggers when player does or takes damage
    @EventHandler
    public void onPlayerPunchEntity(EntityDamageByEntityEvent event) {
        ESPSocketServer server = ESPSync.getInstance().getWebsocketServer();
        if (server == null) return;

        if (event.getDamager() instanceof Player) {
            server.broadcast(buildStatus("Player punched " + event.getEntity().getName()));
        }

        if (event.getEntity() instanceof Player) {
            server.broadcast(buildStatus("Player hit by " + event.getDamager().getName()));
        }
    }

    // this triggers when player dies
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        ESPSocketServer server = ESPSync.getInstance().getWebsocketServer();
        if (server == null) return;

        server.broadcast(buildStatus("Player died"));
    }

    // this triggers when player respawns
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        ESPSocketServer server = ESPSync.getInstance().getWebsocketServer();
        if (server == null) return;

        server.broadcast(buildStatus("Player respawned"));
    }

    // this triggers when player breaks a block
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        ESPSocketServer server = ESPSync.getInstance().getWebsocketServer();
        if (server == null) return;

        Block block = event.getBlock();

        server.broadcast(buildStatus("Player broke " + WordUtils.capitalizeFully(block.getType().name().replaceAll("_", " "))));
    }

    // this triggers when player places a block
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ESPSocketServer server = ESPSync.getInstance().getWebsocketServer();
        if (server == null) return;

        Block block = event.getBlock();

        server.broadcast(buildStatus("Player placed " +  WordUtils.capitalizeFully(block.getType().name().replaceAll("_", " "))));
    }

    // on entity death, if the killer is a player, broadcast a message
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        ESPSocketServer server = ESPSync.getInstance().getWebsocketServer();
        if (server == null) return;

        if (event.getEntity().getKiller() != null) {
            server.broadcast(buildStatus("Player killed " + event.getEntity().getName()));
        }
    }



}
