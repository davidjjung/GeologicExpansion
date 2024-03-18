package com.blackgear.geologicexpansion.common.entity.bear;

import com.blackgear.geologicexpansion.client.events.EntityEvents;
import com.blackgear.geologicexpansion.common.entity.bear.goals.GrizzlyMeleeAttackGoal;
import com.blackgear.geologicexpansion.common.entity.resource.EntityState;
import com.blackgear.geologicexpansion.common.registries.GEEntities;
import com.blackgear.geologicexpansion.common.registries.entities.GEEntityDataSerializers;
import com.blackgear.geologicexpansion.core.data.GEEntityTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class Grizzly extends Animal {
    // ---------- ENTITY DATA ----------
    private static final EntityDataAccessor<Boolean> ATE_RECENTLY = SynchedEntityData.defineId(Grizzly.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<EntityState> STATE = SynchedEntityData.defineId(Grizzly.class, GEEntityDataSerializers.ENTITY_STATE);

    // ---------- ENTITY ANIMATIONS ----------
    public final AnimationState sleepAnimationState = new AnimationState();
    public final AnimationState runningAnimationState = new AnimationState();
    public final AnimationState attackAnimationState = new AnimationState();
    public final AnimationState fishAnimationState = new AnimationState();

    // ---------- ENTITY ATTRIBUTES ----------
    private int ateCooldownTicks = 6000;
    private int ticksSinceEaten;

    // ========== CONSTRUCTOR ==========

    public Grizzly(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
        this.setPathfindingMalus(BlockPathTypes.DANGER_OTHER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_OTHER, 0.0F);
        this.setCanPickUpLoot(true);
    }

    // ========== BEHAVIOR ==========

    public static AttributeSupplier.Builder createAttributes() {
        return AgeableMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.FOLLOW_RANGE, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.125)
            .add(Attributes.ATTACK_DAMAGE, 6.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new GrizzlyMeleeAttackGoal(this, 2.25, true));
//        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 1.0, 60));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, false, false, entity -> {
            return !entity.isBaby() && entity.getType().is(GEEntityTags.GRIZZLY_HUNT_TARGETS);
        }));
    }

    @Override
    public void aiStep() {
        if (!this.level.isClientSide && this.isAlive() && this.isEffectiveAi()) {
            ++this.ticksSinceEaten;
            ItemStack stack = this.getItemBySlot(EquipmentSlot.MAINHAND);
            if (this.canEat(stack)) {
                if (this.ticksSinceEaten > 600) {
                    ItemStack result = stack.finishUsingItem(this.level, this);
                    if (!result.isEmpty()) {
                        this.setItemSlot(EquipmentSlot.MAINHAND, result);
                    }

                    this.ticksSinceEaten = 0;
                } else if (this.ticksSinceEaten > 560 && this.random.nextFloat() < 0.1F) {
                    this.playSound(this.getEatingSound(stack), 1.0F, 1.0F);
                    this.level.broadcastEntityEvent(this, EntityEvents.SPAWN_EATING_PARTICLES);
                }
            }
            
            if (this.ateRecently() && --this.ateCooldownTicks <= 0) {
                this.setAteRecently(false);
                this.ateCooldownTicks = 6000;
            }
        }

        if (this.isSleeping() || this.isImmobile()) {
            this.jumping = false;
            this.xxa = 0.0F;
            this.zza = 0.0F;
        }

        super.aiStep();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level.isClientSide) {
            this.setupAnimationStates();
        }
    }

    private boolean canEat(ItemStack stack) {
        return stack.getItem().isEdible() && this.getTarget() == null && this.onGround && !this.isSleeping();
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return super.isFood(stack);
    }

    @Nullable @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return GEEntities.GRIZZLY.get().create(level);
    }

    // ========== ENTITY EVENTS ==========

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EntityEvents.SPAWN_EATING_PARTICLES) {
            ItemStack stack = this.getItemBySlot(EquipmentSlot.MAINHAND);
            if (!stack.isEmpty()) {
                for (int i = 0; i < 8; i++) {
                    Vec3 motion = new Vec3(((double) this.random.nextFloat() - 0.5) * 0.1, Math.random() * 0.1 + 0.1, 0.0)
                        .xRot(-this.getXRot() * (float) (Math.PI / 180.0))
                        .yRot(-this.getYRot() * (float) (Math.PI / 180.0));
                    this.level.addParticle(
                        new ItemParticleOption(ParticleTypes.ITEM, stack),
                        this.getX() + this.getLookAngle().x / 2.0,
                        this.getY(),
                        this.getZ() + this.getLookAngle().z / 2.0,
                        motion.x,
                        motion.y + 0.05,
                        motion.z
                    );
                }
            }
        }

        super.handleEntityEvent(id);
    }

    // ========== DATA CONTROL ==========


    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setAteRecently(compound.getBoolean("AteRecently"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("AteRecently", this.ateRecently());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATE_RECENTLY, false);
        this.entityData.define(STATE, EntityState.IDLE);
    }

    public void setState(EntityState state) {
        if (state != this.getState()) this.entityData.set(STATE, state);
    }

    public EntityState getState() {
        return this.entityData.get(STATE);
    }

    public void setAteRecently(boolean ate) {
        if (ate != this.ateRecently()) this.entityData.set(ATE_RECENTLY, ate);
    }

    public boolean ateRecently() {
        return this.entityData.get(ATE_RECENTLY);
    }

    // ========== ANIMATION ==========

    private void setupAnimationStates() {
        if (this.getState() == EntityState.ATTACKING) {
            this.attackAnimationState.startIfStopped(this.tickCount);
        } else {
            this.attackAnimationState.stop();
        }
    }

    // ========== MOVEMENT ==========

    public boolean isRunning() {
        return this.moveControl.getSpeedModifier() > 1.0F;
    }

    // ========== SOUNDS ==========

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isBaby() ? SoundEvents.POLAR_BEAR_AMBIENT_BABY : SoundEvents.POLAR_BEAR_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.POLAR_BEAR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.POLAR_BEAR_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.POLAR_BEAR_STEP, 0.15F, 1.0F);
    }
}