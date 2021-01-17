package com.cyborgmas.mobstatues.util;

import net.minecraft.world.World;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

/**
 * Class to use when calling SERVER safe things in {@link DistExecutor}
 */
public class SafeServerClass {
    public static World getWorldOnServer() {
        World ow = ServerLifecycleHooks.getCurrentServer().getWorld(World.OVERWORLD);
        if (ow == null)
            throw new RuntimeException("Could not retrieve ow from the server, method was called to early.");
        return ow;
    }
}
