package com.blackgear.geologicexpansion.common;

import com.blackgear.geologicexpansion.common.entity.duck.Duck;
import com.blackgear.geologicexpansion.common.registries.GEEntities;
import com.blackgear.geologicexpansion.common.worldgen.TerrablenderCompat;
import com.blackgear.geologicexpansion.common.worldgen.WorldGeneration;
import com.blackgear.geologicexpansion.common.worldgen.placements.CalderaFeatures;
import com.blackgear.geologicexpansion.common.worldgen.placements.CalderaPlacements;
import com.blackgear.geologicexpansion.common.worldgen.placements.SurfaceFeatures;
import com.blackgear.geologicexpansion.common.worldgen.placements.SurfacePlacements;
import com.blackgear.geologicexpansion.common.worldgen.placements.UndergroundFeatures;
import com.blackgear.geologicexpansion.common.worldgen.placements.UndergroundPlacements;
import com.blackgear.geologicexpansion.core.mixin.access.SpawnPlacementsAccessor;
import com.blackgear.geologicexpansion.core.platform.common.BiomeManager;
import com.blackgear.geologicexpansion.core.platform.common.EntityRegistry;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

public class CommonSetup {
    public static void onInstance() {
        // ========== ENTITY ATTRIBUTE REGISTRY ========================================================================
        EntityRegistry.attributes(GEEntities.DUCK, Duck::createAttributes);
    }

    public static void postInstance() {
        // ========= FEATURE REGISTRY ==================================================================================
        CalderaFeatures.FEATURES.register();
        CalderaPlacements.FEATURES.register();
        SurfaceFeatures.FEATURES.register();
        SurfacePlacements.FEATURES.register();
        UndergroundFeatures.FEATURES.register();
        UndergroundPlacements.FEATURES.register();

        // ========= WORLD GEN INITIALIZATION ==========================================================================
        BiomeManager.bootstrap();
        WorldGeneration.bootstrap();
        TerrablenderCompat.registerSurfaceRules();

        // ========== ENTITY SPAWN REGISTRY ============================================================================
        SpawnPlacementsAccessor.register(GEEntities.DUCK.get(),
                SpawnPlacements.Type.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Duck::checkDuckSpawnRules
        );
    }
}