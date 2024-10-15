package potato.server;

import potato.Map;

import java.io.Serializable;

public class MapDataPacket implements Packet, Serializable {
    private final int[][] mapData;

    public MapDataPacket(Map map) {
        this.mapData = map.getMapData();
    }

    @Override
    public PacketType getType() {
        return PacketType.MAP_DATA;
    }

    public int[][] getMapData() {
        return mapData;
    }
}