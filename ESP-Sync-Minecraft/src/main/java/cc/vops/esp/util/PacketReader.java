package cc.vops.esp.util;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class PacketReader {
    private String packetType;
    private final HashMap<String, String> fields;

    public PacketReader(String packet) throws IllegalArgumentException {
        fields = new HashMap<>();
        parsePacket(packet);
    }

    private void parsePacket(String packet) throws IllegalArgumentException {
        String[] parts = packet.split("\\|");

        if (parts.length < 1) {
            throw new IllegalArgumentException("Invalid packet format");
        }

        // first part is the packet type
        packetType = parts[0];

        // rest are key-value pairs
        for (int i = 1; i < parts.length; i++) {
            String[] keyValue = parts[i].split("=", 2);
            if (keyValue.length == 2) {
                fields.put(keyValue[0], keyValue[1]);
            } else {
                throw new IllegalArgumentException("Invalid key-value pair in packet: " + parts[i]);
            }
        }
    }

    public String getString(String key) {
        return fields.get(key);
    }

    public int getInt(String key) {
        return Integer.parseInt(fields.get(key));
    }

    public float getFloat(String key) {
        return Float.parseFloat(fields.get(key));
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(fields.get(key));
    }
}
