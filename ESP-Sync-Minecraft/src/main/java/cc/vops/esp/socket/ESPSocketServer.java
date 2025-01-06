package cc.vops.esp.socket;

import cc.vops.esp.ESPSync;
import cc.vops.esp.util.PacketBuilder;
import cc.vops.esp.util.PacketReader;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.HashMap;

import static cc.vops.esp.util.PlayerMapRenderer.renderPlayerMap;

public class ESPSocketServer extends WebSocketServer {
    private Chicken chicken;
    private double chickenDistanceCM;

    public ESPSocketServer(int port) {
        super(new InetSocketAddress(port));
        chickenDistanceCM = 10;

        Bukkit.getScheduler().runTaskTimer(ESPSync.getInstance(), this::chickenTick, 0, 1L);
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        ESPSync.getInstance().getLogger().info("New connection: " + webSocket.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        ESPSync.getInstance().getLogger().info("Closed connection: " + webSocket.getRemoteSocketAddress());

        String name = webSocket.getAttachment();
        if (name != null) {
            // Bukkit.broadcastMessage(ChatColor.RED + name + " has disconnected from the server.");
        }
    }

    public void chickenTick() {
        if (chicken == null) return;

        Player player = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (player == null) return;

        double distance = (chickenDistanceCM / 10);

        // make it so chicken is 10 blocks infront of player eye location
        double x = player.getLocation().getDirection().getX() * distance;
        double y = player.getLocation().getDirection().getY() * distance;
        double z = player.getLocation().getDirection().getZ() * distance;

        chicken.teleport(player.getLocation().add(x, y, z));

        // draw particle line to chicken from player
        for (double i = 0; i < distance; i += 0.1) {
            double px = player.getLocation().getX() + player.getLocation().getDirection().getX() * i;
            double py = player.getLocation().getY() + player.getLocation().getDirection().getY() * i;
            double pz = player.getLocation().getZ() + player.getLocation().getDirection().getZ() * i;

            player.spawnParticle(Particle.DUST, px, py, pz, 0, 0, 0, 0, 1, new Particle.DustOptions(Color.PURPLE, 0.5f));
        }

        // subtitle to player
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GREEN + "Chicken is " + Math.round(chickenDistanceCM) + "cm away"));
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        ESPSync.getInstance().getLogger().info("Received message: " + s);

        PacketReader packet = new PacketReader(s);

        if (packet.getPacketType().equals("identity")) {
            String name = packet.getString("name");
            webSocket.setAttachment(name);
            Bukkit.broadcastMessage(ChatColor.GREEN + name + " has connected to the server.");
        }

        // 6000 = noon, 18000 = midnight
        if (packet.getPacketType().equals("time")) {
            long time = Long.parseLong(packet.getString("time"));

            Bukkit.getScheduler().runTask(ESPSync.getInstance(), () -> {
                Bukkit.getWorlds().forEach(world -> world.setTime(time));
            });
        }

        if (packet.getPacketType().equals("broadcast")) {
            String message = packet.getString("message");
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
        }

        if (packet.getPacketType().equals("weather")) {
            String weatherType = packet.getString("type");

            Bukkit.getScheduler().runTask(ESPSync.getInstance(), () -> {
                switch (weatherType) {
                    case "clear":
                        Bukkit.getWorlds().forEach(world -> world.setStorm(false));
                        Bukkit.getWorlds().forEach(world -> world.setThundering(false));
                        break;
                    case "rain":
                        Bukkit.getWorlds().forEach(world -> world.setStorm(true));
                        Bukkit.getWorlds().forEach(world -> world.setThundering(false));
                        break;
                    case "thunder":
                        Bukkit.getWorlds().forEach(world -> world.setStorm(true));
                        Bukkit.getWorlds().forEach(world -> world.setThundering(true));
                        break;
                }
            });
        }

        if (packet.getPacketType().equals("chickenDistance")) {
            chickenDistanceCM = Integer.parseInt(packet.getString("distance"));
        }

        Player player = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (player == null) return;

        if (packet.getPacketType().equals("requestMap")) {
            broadcast(renderPlayerMap(player));
        }

        if (packet.getPacketType().equals("boom")) {
            Bukkit.getScheduler().runTask(ESPSync.getInstance(), () -> {
                player.getWorld().createExplosion(player.getLocation(), 10.0f);
            });
        }

        if (packet.getPacketType().equals("spawnChicken")) {
            Bukkit.getScheduler().runTask(ESPSync.getInstance(), () -> {
                if (chicken != null) {
                    chicken.remove();
                }

                chicken = player.getWorld().spawn(player.getLocation(), Chicken.class);
                chicken.setGravity(false);
                chicken.setAI(false);
                chicken.setGlowing(true);
                chicken.setInvulnerable(true);
                chicken.setSilent(true);
                chicken.setCollidable(false);
                chicken.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 255, false, false, false));
            });
        }

        if (packet.getPacketType().equals("killChicken")) {
            Bukkit.getScheduler().runTask(ESPSync.getInstance(), () -> {
                if (chicken != null) {
                    chicken.remove();
                }

                chicken = null;
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
            });
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        ESPSync.getInstance().getLogger().severe("Error in connection: " + webSocket.getRemoteSocketAddress());
        e.printStackTrace();
    }

    @Override
    public void onStart() {
        ESPSync.getInstance().getLogger().info("WebSocket server started");
    }

    public void broadcast(String message) {
        for (WebSocket webSocket : getConnections()) {
            webSocket.send(message);
        }
    }

    public void tick() {
        Player player = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (player == null) return;

        PacketBuilder packet = new PacketBuilder("playerInfo")
                .addField("name", player.getName())
                .addField("health", player.getHealth())
                .addLocation(player.getLocation());

        broadcast(packet.build());
    }
}
