package com.cyborgmas.mobstatues.registration;

import com.cyborgmas.mobstatues.MobStatues;
import com.cyborgmas.mobstatues.client.StatueTileRenderer;
import com.cyborgmas.mobstatues.objects.DelegatingTileEntity;
import com.cyborgmas.mobstatues.objects.StatueBlock;
import com.cyborgmas.mobstatues.objects.StatueBlockItem;
import com.cyborgmas.mobstatues.objects.StatueTileEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class Registration {
    public static DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MobStatues.MODID);
    public static DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MobStatues.MODID);
    public static DeferredRegister<TileEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, MobStatues.MODID);

    public static RegistryObject<Block> STATUE_BLOCK = BLOCKS.register("statue_block", () ->
            new StatueBlock(
                    AbstractBlock.Properties.create(Material.ROCK)
                            .setAllowsSpawn((s, r, p, e) -> false)
                            .notSolid()
                            .setOpaque((s, r, p) -> false)
                            .variableOpacity() // makes it not cache the collision boxes and whatnot.
            )
    );

    public static RegistryObject<Item> STATUE_ITEM = ITEMS.register("statue", () ->
            new StatueBlockItem(
                    new Item.Properties()
                            .maxStackSize(1)
                            .group(ItemGroup.DECORATIONS)
                            .setISTER(() -> () -> StatueTileRenderer.getStatueItemRenderer())
            )
    );

    public static RegistryObject<TileEntityType<StatueTileEntity>> STATUE_TILE =
            TILE_ENTITIES.register("statue", () -> TileEntityType.Builder.create(StatueTileEntity::new, STATUE_BLOCK.get()).build(null));
    public static RegistryObject<TileEntityType<DelegatingTileEntity>> DELEGATING_TILE =
            TILE_ENTITIES.register("delegating", () -> TileEntityType.Builder.create(DelegatingTileEntity::new, STATUE_BLOCK.get()).build(null));

    public static void registerAll(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TILE_ENTITIES.register(bus);
    }
}
