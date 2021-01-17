package com.cyborgmas.mobstatues.client;

import com.cyborgmas.mobstatues.network.SpawnWrapperPacket;
import com.cyborgmas.mobstatues.objects.StatueTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPartEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientEntityReceiver {
    public static boolean handle(SpawnWrapperPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            World world = Minecraft.getInstance().world;
            if (world == null)
                return;
            TileEntity te = world.getTileEntity(packet.getTileEntity());
            if (te instanceof StatueTileEntity && !te.isRemoved()) {
                StatueTileEntity statue = (StatueTileEntity) te;
                EntityType<?> type = statue.getEntityType();
                if (type == null)
                    return;
                Entity e = type.create(world);
                if (e == null)
                    return;

                // Copied from ClientPlayNetHandler#handleMobSpawn
                double x = packet.getSpawnPacket().getX();
                double y = packet.getSpawnPacket().getY();
                double z = packet.getSpawnPacket().getZ();
                float yaw = (float)(packet.getSpawnPacket().getYaw() * 360) / 256.0F;
                float pitch = (float)(packet.getSpawnPacket().getPitch() * 360) / 256.0F;

                e.setPacketCoordinates(x, y, z);

                if (e instanceof LivingEntity) {
                    LivingEntity le = (LivingEntity) e;
                    le.renderYawOffset = (float)(packet.getSpawnPacket().getHeadPitch() * 360) / 256.0F;
                    le.rotationYawHead = (float)(packet.getSpawnPacket().getHeadPitch() * 360) / 256.0F;
                }

                if (e instanceof EnderDragonEntity) {
                    EnderDragonPartEntity[] aenderdragonpartentity = ((EnderDragonEntity)e).getDragonParts();
                    for(int i = 0; i < aenderdragonpartentity.length; ++i)
                        aenderdragonpartentity[i].setEntityId(i + packet.getSpawnPacket().getEntityID());
                }

                e.setPositionAndRotation(x, y, z, yaw, pitch);
                e.setMotion(
                        ((float)packet.getSpawnPacket().getVelocityX() / 8000.0F),
                        ((float)packet.getSpawnPacket().getVelocityY() / 8000.0F),
                        ((float)packet.getSpawnPacket().getVelocityZ() / 8000.0F)
                );

                if (packet.getMetadataPacket().getDataManagerEntries() != null)
                    e.getDataManager().setEntryValues(packet.getMetadataPacket().getDataManagerEntries());

                statue.setEntity(e);
            }
        });
        return true;
    }
}
