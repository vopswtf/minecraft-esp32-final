package cc.vops.esp.util;

// example packets =
// identity|name=ESP32
// playerInfo|name=ESP32|health=20|food=20|armor=0|location=0,0,0
import org.bukkit.Location;

import java.util.HashMap;

// make it dynamic so we can add more packets later
public class PacketBuilder {
    private final String packetType;
    private final HashMap<String, String> fields;

    public PacketBuilder(String packetType) {
        this.packetType = packetType;
        this.fields = new HashMap<>();
    }

    public PacketBuilder addField(String key, String value) {
        fields.put(key, value);
        return this;
    }

    public PacketBuilder addField(String key, int value) {
        return addField(key, Integer.toString(value));
    }

    public PacketBuilder addField(String key, float value) {
        return addField(key, Float.toString(value));
    }

    public PacketBuilder addField(String key, double value) {
        return addField(key, Double.toString(value));
    }

    public PacketBuilder addField(String key, boolean value) {
        return addField(key, Boolean.toString(value));
    }

    public PacketBuilder addLocation(Location location) {
        return addField("location", location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
    }

    public String build() {
        StringBuilder packet = new StringBuilder(packetType + "|");

        for (HashMap.Entry<String, String> entry : fields.entrySet()) {
            packet.append(entry.getKey()).append("=").append(entry.getValue()).append("|");
        }

        // remove the | at the end
        if (packet.charAt(packet.length() - 1) == '|') {
            packet.deleteCharAt(packet.length() - 1);
        }

        return packet.toString();
    }
}
