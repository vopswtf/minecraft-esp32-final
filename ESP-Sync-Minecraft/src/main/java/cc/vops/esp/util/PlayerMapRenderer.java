package cc.vops.esp.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class PlayerMapRenderer {
    public static Block getHighestBlockAt(int x, int z, World world) {
        Block highestBukkit = world.getHighestBlockAt(x, z);

        for (int i = highestBukkit.getY(); i > 0; i--) {
            Block block = world.getBlockAt(x, i, z);
            if (block.getType() == Material.AIR) continue;
            if (block.getType() == Material.BARRIER) continue;
            if (block.getType() == Material.GLASS) continue;
            if (block.getType().name().contains("SIGN")) continue;
            if (block.getType().isTransparent()) continue;

            highestBukkit = block;
            break;
        }

        return highestBukkit;
    }

    private static final int MAP_RADIUS = 12;

    public static String renderPlayerMap(Player player) {
        // Player is in center
        JsonObject mapJson = new JsonObject();

        // We start negative MAP_RADIUS blocks from player and go to positive MAP_RADIUS blocks
        for (int x = -MAP_RADIUS; x <= MAP_RADIUS; x++) {
            for (int z = -MAP_RADIUS; z <= MAP_RADIUS; z++) {
                Block block = getHighestBlockAt(player.getLocation().getBlockX() + x, player.getLocation().getBlockZ() + z, player.getWorld());
                Color color = block.getBlockData().getMapColor();

                // spawn particles
                block.getWorld().spawnParticle(
                        Particle.DUST,
                        block.getLocation().add(0.5, 1, 0.5),
                        0, 0, 0, 0, 1,
                        new Particle.DustOptions(Color.PURPLE, 0.5f)
                );

                // convert color to hex
                int rgb = color.asRGB();
                String hexColor = String.format("%06x", rgb & 0xFFFFFF);

                // store in json
                String key = (x + MAP_RADIUS) + "," + (z + MAP_RADIUS);
                mapJson.addProperty(key, hexColor);
            }
        }

        return new PacketBuilder("playerMap")
                .addField("map", mapJson.toString())
                .build();
    }

    private static boolean hasBlockAt(World world, int x, int z) {
        Block block = getHighestBlockAt(x, z, world);
        return block != null && block.getType() != Material.AIR;
    }
}
