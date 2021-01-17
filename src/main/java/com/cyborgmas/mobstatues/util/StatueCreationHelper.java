package com.cyborgmas.mobstatues.util;

import com.cyborgmas.mobstatues.MobStatues;
import com.google.common.collect.ImmutableList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Pose;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Util;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.World;
import net.minecraftforge.fml.DistExecutor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class StatueCreationHelper {
    private static final List<EntityType<?>> DYNAMIC_SIZED_ENTITIES = Util.make(() -> ImmutableList.<EntityType<?>>builder()
            .add(EntityType.MAGMA_CUBE)
            .build()
    );

    private static final Map<EntityType<?>, VoxelShape> STATIC_SHAPE = new HashMap<>();
    private static final Map<EntityType<?>, EntitySize> STATIC_SIZE = new HashMap<>();
    private static final Map<EntityType<?>, Entity> STATIC_ENTITY_MODEL = new HashMap<>();

    public static Getter<Entity> getEntity(CompoundNBT nbt) {
        return getEntity(nbt, false);
    }

    public static Getter<Entity> getEntity(CompoundNBT nbt, boolean noCache) {
        return new Getter<>(nbt,
                type -> {
                    if (noCache)
                        return createEntity(type, getWorld());
                    return STATIC_ENTITY_MODEL.computeIfAbsent(type, t -> createEntity(t, getWorld()));
                },
                Function.identity()
        );
    }

    public static Getter<EntitySize> getEntitySize(CompoundNBT nbt) {
        return new Getter<>(nbt,
                type -> STATIC_SIZE.computeIfAbsent(type, EntityType::getSize),
                e -> e.getSize(Pose.STANDING)
        );
    }

    public static Getter<VoxelShape> getShape(CompoundNBT nbt) {
        return new Getter<>(nbt,
                type -> STATIC_SHAPE.computeIfAbsent(type, t ->
                        VoxelShapes.create(t.getBoundingBoxWithSizeApplied(0, 0,0))),
                e -> VoxelShapes.create(e.getBoundingBox())
        );
    }

    public static World getWorld() {
        return DistExecutor.safeRunForDist(() -> SafeClientClass::getWorldOnClient, () -> SafeServerClass::getWorldOnServer);
    }

    public static Entity createEntity(EntityType<?> type, World world) {
        if (world == null)
            world = getWorld();
        return type.create(world);
    }

    public static Entity createEntity(CompoundNBT nbt, World world) {
        if (world == null)
            world = getWorld();
        return EntityType.readEntityType(nbt).orElse(EntityType.PIG).create(world);
    }

    public static Entity createEntityAndRead(CompoundNBT nbt, World world) {
        if (world == null)
            world = getWorld();
        Entity entity =  EntityType.readEntityType(nbt).orElse(EntityType.PIG).create(world);

        if (entity != null) {
            try {
                entity.read(nbt);
            } catch (Exception e) {
                MobStatues.LOGGER.warn("Could not read nbt for entity of type {}, reading should never make assumptions about the tag.", entity.getType().getRegistryName(), e);
            }
        }
        return entity;
    }

    /**
     * Probably dumb?
     */
    public static class Getter<T> implements Supplier<T> {
        /**
         * A fall back in case of no context. Otherwise use {@link #withWorld}
         */
        private Supplier<World> world = StatueCreationHelper::getWorld;
        private final CompoundNBT nbt;
        private final Function<EntityType<?>, T> staticGet;
        private final Function<Entity, T> dynamicGet;

        /**
         * Provides a way to get an object based on whether the entity needs to be created beforehand (dynamic)
         * or not (static).
         *
         * When static, this can be cached.
         *
         * Caching the dynamic result is still done down the line, but is much more volatile.
         *
         * @param nbt           NBT containing the EntityType or data for the Entity.
         * @param staticGet     How to get the object from the EntityType
         * @param dynamicGet    How to get the object from the Entity
         */
        private Getter(CompoundNBT nbt, Function<EntityType<?>, T> staticGet, Function<Entity, T> dynamicGet) {
            this.nbt = nbt;
            this.staticGet = staticGet;
            this.dynamicGet = dynamicGet;
        }

        @Override
        public T get() {
            EntityType<? extends Entity> type = EntityType.readEntityType(nbt).orElse(EntityType.PIG);

            if (DYNAMIC_SIZED_ENTITIES.contains(type)){
                Entity e = createEntityAndRead(nbt, this.world.get());
                if (e != null)
                    return this.dynamicGet.apply(e);
            }

            return this.staticGet.apply(type);
        }

        public T withWorld(World world) {
            if (world != null)
                this.world = () -> world;
            return get();
        }
    }
}
