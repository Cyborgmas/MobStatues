package com.cyborgmas.mobstatues.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.thread.EffectiveSide;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

/**
 * Class to use when calling CLIENT safe things in {@link DistExecutor}
 */
public class SafeClientClass {
    public static World getWorldOnClient() {
        if (EffectiveSide.get().isClient())
            return Minecraft.getInstance().world;
        return ServerLifecycleHooks.getCurrentServer().getWorld(World.OVERWORLD);
    }
}
