package com.cyborgmas.mobstatues.network;

import com.cyborgmas.mobstatues.MobStatues;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SEntityMetadataPacket;
import net.minecraft.network.play.server.SSpawnMobPacket;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;

public class SpawnWrapperPacket {
    private SSpawnMobPacket spawnPacket;
    private SEntityMetadataPacket metadataPacket;
    private BlockPos tileEntity;

    public SpawnWrapperPacket() {
        this.spawnPacket = new SSpawnMobPacket();
        this.metadataPacket = new SEntityMetadataPacket();
    }

    public SpawnWrapperPacket(SSpawnMobPacket spawnPacket, SEntityMetadataPacket metadataPacket, BlockPos pos) {
        this.spawnPacket = spawnPacket;
        this.metadataPacket = metadataPacket;
        this.tileEntity = pos;
    }

    public void encode(PacketBuffer buffer) {
        try {
            this.spawnPacket.writePacketData(buffer);
            this.metadataPacket.writePacketData(buffer);
            buffer.writeLong(tileEntity.toLong());
        } catch (IOException e) {
            MobStatues.LOGGER.error("Could not send entity data packet.");
        }
    }

    public static SpawnWrapperPacket decode(PacketBuffer buffer) {
        SpawnWrapperPacket ret = new SpawnWrapperPacket();
        try {
            ret.spawnPacket.readPacketData(buffer);
            ret.metadataPacket.readPacketData(buffer);
            ret.tileEntity = BlockPos.fromLong(buffer.readLong());
        } catch (IOException e) {
            MobStatues.LOGGER.error("Could not process received entity data packet.");
        }
        return ret;
    }

    // getters for client
    public SSpawnMobPacket getSpawnPacket(){
        return spawnPacket;
    }

    public SEntityMetadataPacket getMetadataPacket(){
        return metadataPacket;
    }

    public BlockPos getTileEntity(){
        return tileEntity;
    }
}
