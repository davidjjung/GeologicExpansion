package com.blackgear.geologicexpansion.data.common;

import com.blackgear.geologicexpansion.common.registries.GEBlocks;
import com.blackgear.geologicexpansion.data.resources.GEBlockFamilies;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.function.Consumer;

public class RecipeGenerator extends FabricRecipeProvider {
    public RecipeGenerator(FabricDataGenerator generator) {
        super(generator);
    }

    @Override
    protected void generateRecipes(Consumer<FinishedRecipe> exporter) {
        GEBlockFamilies.getAllFamilies()
            .filter(BlockFamily::shouldGenerateRecipe)
            .forEach(family -> generateRecipes(exporter, family));
        GEBlockFamilies.getStoneFamilies()
            .forEach(family -> generateStonecutterRecipes(exporter, family));

        generateDyedPrismaticStone(exporter, GEBlocks.WHITE_PRISMATIC_STONE.get(), Items.WHITE_DYE);
        generateDyedPrismaticStone(exporter, GEBlocks.ORANGE_PRISMATIC_STONE.get(), Items.ORANGE_DYE);
        generateDyedPrismaticStone(exporter, GEBlocks.MAGENTA_PRISMATIC_STONE.get(), Items.MAGENTA_DYE);
        generateDyedPrismaticStone(exporter, GEBlocks.LIGHT_BLUE_PRISMATIC_STONE.get(), Items.LIGHT_BLUE_DYE);
        generateDyedPrismaticStone(exporter, GEBlocks.YELLOW_PRISMATIC_STONE.get(), Items.YELLOW_DYE);
        generateDyedPrismaticStone(exporter, GEBlocks.LIME_PRISMATIC_STONE.get(), Items.LIME_DYE);
        generateDyedPrismaticStone(exporter, GEBlocks.PINK_PRISMATIC_STONE.get(), Items.PINK_DYE);
        generateDyedPrismaticStone(exporter, GEBlocks.GRAY_PRISMATIC_STONE.get(), Items.GRAY_DYE);
        generateDyedPrismaticStone(exporter, GEBlocks.LIGHT_GRAY_PRISMATIC_STONE.get(), Items.LIGHT_GRAY_DYE);
        generateDyedPrismaticStone(exporter, GEBlocks.CYAN_PRISMATIC_STONE.get(), Items.CYAN_DYE);
        generateDyedPrismaticStone(exporter, GEBlocks.PURPLE_PRISMATIC_STONE.get(), Items.PURPLE_DYE);
        generateDyedPrismaticStone(exporter, GEBlocks.BLUE_PRISMATIC_STONE.get(), Items.BLUE_DYE);
        generateDyedPrismaticStone(exporter, GEBlocks.BROWN_PRISMATIC_STONE.get(), Items.BROWN_DYE);
        generateDyedPrismaticStone(exporter, GEBlocks.GREEN_PRISMATIC_STONE.get(), Items.GREEN_DYE);
        generateDyedPrismaticStone(exporter, GEBlocks.RED_PRISMATIC_STONE.get(), Items.RED_DYE);
        generateDyedPrismaticStone(exporter, GEBlocks.BLACK_PRISMATIC_STONE.get(), Items.BLACK_DYE);

        ShapedRecipeBuilder.shaped(GEBlocks.GEOLOGIST_TABLE.get())
                .define('#', ItemTags.PLANKS)
                .define('X', Items.AMETHYST_SHARD)
                .pattern("XX")
                .pattern("##")
                .pattern("##")
                .unlockedBy("has_amethyst_shard", has(Items.AMETHYST_SHARD))
                .save(exporter);
    }

    private void generateDyedPrismaticStone(Consumer<FinishedRecipe> exporter, ItemLike colored, ItemLike dye) {
        ShapedRecipeBuilder
                .shaped(colored, 8)
                .define('#', GEBlocks.PRISMATIC_STONE.get())
                .define('X', dye)
                .pattern("###")
                .pattern("#X#")
                .pattern("###")
                .group("prismatic_stone")
                .unlockedBy("has_prismatic_stone", has(GEBlocks.PRISMATIC_STONE.get()))
                .save(exporter);
    }

    private void generateStonecutterRecipes(Consumer<FinishedRecipe> exporter, BlockFamily family) {
        for (BlockFamily.Variant variant : family.getVariants().keySet()) {
            int amount = variant == BlockFamily.Variant.SLAB ? 2 : 1;
            stonecutterResultFromBase(exporter, family.get(variant), family.getBaseBlock(), amount);
        }
    }
}