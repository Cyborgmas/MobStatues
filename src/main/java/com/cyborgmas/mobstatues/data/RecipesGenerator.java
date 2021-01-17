package com.cyborgmas.mobstatues.data;

import com.cyborgmas.mobstatues.registration.Registration;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.data.RecipeProvider;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Util;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.minecraft.entity.EntityType.*;
import static net.minecraft.item.Items.*;

public class RecipesGenerator extends RecipeProvider {
    //Only initialised after mods so static init is fine!
    private static final ItemStack BASE = new ItemStack(Registration.STATUE_ITEM.get());
    private static final Map<EntityType<?>, Item> SIMPLE_RECIPES = Util.make(() -> new ImmutableMap.Builder<EntityType<?>, Item>()
            .put(CREEPER, GUNPOWDER)
            .put(ENDERMAN, Items.ENDER_PEARL)
            .put(ZOMBIE, ROTTEN_FLESH)
            .put(COW, LEATHER)
            .put(SPIDER, SPIDER_EYE)
            .put(IRON_GOLEM, IRON_BLOCK)
            .put(ELDER_GUARDIAN, SEA_LANTERN)
            .put(GUARDIAN, PRISMARINE_SHARD)
            .put(BOAT, OAK_BOAT)
            .put(PARROT, COOKIE)
            .put(CAT, Items.COD)
            .put(WITHER, WITHER_ROSE)
            .put(BLAZE, BLAZE_ROD)
            .put(GIANT, NETHER_STAR)
            .put(ZOGLIN, GOLD_NUGGET)
            .put(ZOMBIFIED_PIGLIN, GOLD_BLOCK)
            .build()
    );

    private static final List<RecipeMaker> RECIPE_MAKERS = Util.make(() -> new ImmutableList.Builder<RecipeMaker>()
            .addAll(SIMPLE_RECIPES.entrySet().stream().map(e -> simple(e.getKey(), e.getValue())).collect(Collectors.toList()))
            .add(complex(MAGMA_CUBE, MAGMA_CREAM, stack -> stack.getOrCreateTag().putInt("Size", 1)))
            .add(weightedComplexNamed(MAGMA_CUBE, MAGMA_CREAM, 4, "magma_cube_2",stack -> stack.getOrCreateTag().putInt("Size", 2)))
            .add(weightedComplexNamed(MAGMA_CUBE, MAGMA_CREAM, 6, "magma_cube_3",stack -> stack.getOrCreateTag().putInt("Size", 3)))
            .add(weightedComplexNamed(MAGMA_CUBE, MAGMA_CREAM, 8, "magma_cube_4",stack -> stack.getOrCreateTag().putInt("Size", 4)))
            .build()
    );

    public RecipesGenerator(DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    protected void registerRecipes(Consumer<IFinishedRecipe> f) {
        RECIPE_MAKERS.forEach(rm -> rm.makeRecipe(f));
    }

    private static ItemStack putId(EntityType<?> type) {
        ItemStack ret = BASE.copy();
        ret.getOrCreateTag().putString("id", type.getRegistryName().toString());
        return ret;
    }

    private static RecipeMaker simple(EntityType<?> model, Item ingredient) {
        return complex(model, ingredient, s -> {});
    }

    private static RecipeMaker complex(EntityType<?> model, Item ingredient, Consumer<ItemStack> dataSetter) {
        return weightedComplex(model, ingredient, 2, dataSetter);
    }

    private static RecipeMaker weighted(EntityType<?> model, Item ingredient, int nb) {
        return weightedComplex(model, ingredient, nb, s -> {});
    }

    private static RecipeMaker weightedComplex(EntityType<?> model, Item ingredient, int nb, Consumer<ItemStack> dataSetter) {
        return weightedComplexNamed(model, ingredient, nb, model.getRegistryName().getPath(), dataSetter);
    }

    private static RecipeMaker weightedComplexNamed(EntityType<?> model, Item ingredient, int nb, String name, Consumer<ItemStack> dataSetter) {
        return f ->
                WithNBTShapelessRecipeBuilder.shapelessRecipe(Util.make(putId(model), dataSetter))
                        .addIngredient(ingredient, nb)
                        .addIngredient(Registration.STATUE_ITEM.get())
                        .addCriterion("has_statue", hasItem(Registration.STATUE_ITEM.get()))
                        .addCriterion("has_" + ingredient.getRegistryName().toString(), hasItem(ingredient))
                        .build(f, name  + "_mob_statue");
    }

    @FunctionalInterface
    interface RecipeMaker {
        void makeRecipe(Consumer<IFinishedRecipe> f);
    }
}
