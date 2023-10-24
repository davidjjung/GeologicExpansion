package com.blackgear.geologicexpansion.common.entity.duck;

import com.blackgear.geologicexpansion.common.entity.duck.behavior.DuckFishGoal;
import com.blackgear.geologicexpansion.common.entity.resource.FluidWalker;
import com.blackgear.geologicexpansion.common.registries.GEEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Duck extends Animal implements FluidWalker {
    private static final EntityDataAccessor<Boolean> DATA_CAN_FISH = SynchedEntityData.defineId(Duck.class, EntityDataSerializers.BOOLEAN);
    public final AnimationState floatTransformationState = new AnimationState();
    public float flap;
    public float flapSpeed;
    public float oFlapSpeed;
    public float oFlap;
    private float flapping = 1.0F;
    private float nextFlap = 1.0F;

    private int eatAnimationTick;
    private DuckFishGoal fishGoal;


    public Duck(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.LUCK);
    }

    // ========= DATA CONTROL ==========================================================================================

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_CAN_FISH, false);
    }

    public void setCanFish(boolean canFish) {
        this.entityData.set(DATA_CAN_FISH, canFish);
    }

    public boolean canFish() {
        return this.entityData.get(DATA_CAN_FISH);
    }

    // ========== BEHAVIOR =============================================================================================

    @Override
    protected void registerGoals() {
        this.fishGoal = new DuckFishGoal(this);
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, Ingredient.of(Items.COD), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(5, this.fishGoal);
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 1.0D, 60));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Duck.class, 6.0F));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.isInWaterOrBubble()) {
            this.floatTransformationState.startIfStopped(this.tickCount);
        } else {
            this.floatTransformationState.stop();
        }

        this.setCanFish(this.calculateOpenWater(this.blockPosition()) && !this.isBaby());
        this.floatDuck();
        this.checkInsideBlocks();
    }

    @Override
    protected void customServerAiStep() {
        this.eatAnimationTick = this.fishGoal.getEatAnimationTick();
        super.customServerAiStep();
    }

    @Override
    public void aiStep() {
        if (this.level.isClientSide) {
            this.eatAnimationTick = Math.max(0, this.eatAnimationTick - 1);
        }

        super.aiStep();
        this.oFlap = this.flap;
        this.oFlapSpeed = this.flapSpeed;
        this.flapSpeed += (this.onGround ? -1.0F : 4.0F) * 0.3F;
        this.flapSpeed = Mth.clamp(this.flapSpeed, 0.0F, 1.0F);

        if (!this.onGround && this.flapping < 1.0F) {
            this.flapping = 1.0F;
        }

        this.flapping *= 0.9F;
        Vec3 movement = this.getDeltaMovement();
        if (!this.onGround && movement.y < 0.0) {
            this.setDeltaMovement(movement.multiply(1.0, 0.6, 1.0));
        }

        this.flap += this.flapping * 2.0F;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 10) {
            this.eatAnimationTick = 40;
        } else {
            super.handleEntityEvent(id);
        }
    }

    public float getHeadEatPositionScale(float partialTick) {
        if (this.eatAnimationTick <= 0) {
            return 0.0F;
        } else if (this.eatAnimationTick >= 4 && this.eatAnimationTick <= 36) {
            return 1.0F;
        } else {
            return this.eatAnimationTick < 4 ? ((float)this.eatAnimationTick - partialTick) / 4.0F : -((float)(this.eatAnimationTick - 40) - partialTick) / 4.0F;
        }
    }

    public float getHeadEatAngleScale(float partialTick) {
        if (this.eatAnimationTick > 4 && this.eatAnimationTick <= 36) {
            float f = ((float)(this.eatAnimationTick - 4) - partialTick) / 32.0F;
            return (float) (Math.PI / 5) + 0.21991149F * Mth.sin(f * 28.7F);
        } else {
            return this.eatAnimationTick > 0 ? (float) (Math.PI / 5) : this.getXRot() * (float) (Math.PI / 180.0);
        }
    }

    @Override
    public void ate() {
        super.ate();
        this.retrieve();
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return this.isBaby() ? dimensions.height * 0.85F : dimensions.height * 0.92F;
    }

    protected boolean isFlapping() {
        return this.flyDist > this.nextFlap;
    }

    protected void onFlap() {
        this.nextFlap = this.flyDist + this.flapSpeed / 2.0F;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override @Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return GEEntities.DUCK.get().create(level);
    }

    // ========== FLOAT BEHAVIOR =======================================================================================

    private void floatDuck() {
        if (this.isInWater()) {
            CollisionContext context = CollisionContext.of(this);
            if (context.isAbove(this.getStableLiquidShape(), this.blockPosition(), true) && !this.level.getFluidState(this.blockPosition().above()).is(FluidTags.WATER)) {
                this.onGround = true;
            } else {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5D).add(0.0D, 0.05D, 0.0D));
            }
        }
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        if (level.getBlockState(pos).getFluidState().is(FluidTags.WATER)) {
            return 10.0F;
        } else {
            return this.isInWater() ? Float.NEGATIVE_INFINITY : 0.0F;
        }
    }

    @Override
    public boolean canStandOnFluid(FluidState fluidState) {
        return fluidState.is(FluidTags.WATER);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public VoxelShape getStableLiquidShape() {
        return Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);
    }

    // ========== SOUNDS ===============================================================================================

    @Override @Nullable
    protected SoundEvent getAmbientSound() {
        return super.getAmbientSound();
    }

    @Override @Nullable
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return super.getHurtSound(damageSource);
    }

    @Override @Nullable
    protected SoundEvent getDeathSound() {
        return super.getDeathSound();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        super.playStepSound(pos, state);
    }

    // ========== FISHING BEHAVIOR =====================================================================================

    private void retrieve() {
        MinecraftServer server = this.level.getServer();
        if (!this.level.isClientSide && server != null) {
            LootContext.Builder builder = new LootContext.Builder((ServerLevel)this.level)
                    .withParameter(LootContextParams.ORIGIN, this.position())
                    .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
                    .withParameter(LootContextParams.THIS_ENTITY, this)
                    .withRandom(this.random)
                    .withLuck((float)this.getAttributeValue(Attributes.LUCK));
            LootTable lootTable = server.getLootTables().get(BuiltInLootTables.FISHING);
            List<ItemStack> items = lootTable.getRandomItems(builder.create(LootContextParamSets.FISHING));

            for (ItemStack item : items) {
                this.spawnAtLocation(item, 0.25F);
                break;
            }
        }
    }

    private boolean calculateOpenWater(BlockPos pos) {
        OpenWaterType currentType = OpenWaterType.INVALID;

        for (int i = -1; i <= 2; i++) {
            OpenWaterType newType = this.getOpenWaterTypeForArea(pos.offset(-2, i, -2), pos.offset(2, i, 2));
            switch (newType) {
                case INVALID -> {
                    return false;
                }
                case ABOVE_WATER -> {
                    if (currentType == OpenWaterType.INVALID) {
                        return false;
                    }
                }
                case INSIDE_WATER -> {
                    if (currentType == OpenWaterType.ABOVE_WATER) {
                        return false;
                    }
                }
            }

            currentType = newType;
        }

        return true;
    }

    private OpenWaterType getOpenWaterTypeForArea(BlockPos posA, BlockPos posB) {
        return BlockPos.betweenClosedStream(posA, posB).map(this::getOpenWaterTypeForBlock).reduce((openWaterTypeA, openWaterTypeB) -> {
            return openWaterTypeA == openWaterTypeB ? openWaterTypeA : OpenWaterType.INVALID;
        }).orElse(OpenWaterType.INVALID);
    }

    private OpenWaterType getOpenWaterTypeForBlock(BlockPos pos) {
        BlockState state = this.level.getBlockState(pos);
        if (!state.isAir() && !state.is(Blocks.LILY_PAD)) {
            FluidState fluidState = state.getFluidState();
            return fluidState.is(FluidTags.WATER) &&
                    fluidState.isSource() &&
                    state.getCollisionShape(this.level, pos).isEmpty() ?
                    OpenWaterType.INSIDE_WATER : OpenWaterType.INVALID;
        } else {
            return OpenWaterType.ABOVE_WATER;
        }
    }

    enum OpenWaterType {
        ABOVE_WATER,
        INSIDE_WATER,
        INVALID
    }
}