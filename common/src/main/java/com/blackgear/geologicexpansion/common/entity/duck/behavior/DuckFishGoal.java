package com.blackgear.geologicexpansion.common.entity.duck.behavior;

import com.blackgear.geologicexpansion.common.entity.duck.Duck;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.EnumSet;

public class DuckFishGoal extends Goal {
    private static final int EAT_ANIMATION_TICKS = 40;
    private final Duck duck;
    private final Level level;
    private int eatAnimationTick;

    public DuckFishGoal(Duck duck) {
        this.duck = duck;
        this.level = duck.level;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.duck.getRandom().nextInt(1000) != 0) {
            return false;
        } else {
            BlockPos pos = this.duck.blockPosition();
            return this.level.getBlockState(pos.below()).is(Blocks.WATER) && this.duck.canFish();
        }
    }

    @Override
    public void start() {
        this.eatAnimationTick = this.adjustedTickDelay(EAT_ANIMATION_TICKS);
        this.level.broadcastEntityEvent(this.duck, (byte)10);
        this.duck.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.eatAnimationTick = 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.eatAnimationTick > 0;
    }

    public int getEatAnimationTick() {
        return this.eatAnimationTick;
    }

    @Override
    public void tick() {
        this.eatAnimationTick = Math.max(0, this.eatAnimationTick - 1);
        if (this.eatAnimationTick == this.adjustedTickDelay(4)) {
            BlockPos pos = this.duck.blockPosition().below();
            if (this.level.getBlockState(pos).is(Blocks.WATER) && this.duck.canFish()) {
                this.level.levelEvent(2001, pos, Block.getId(Blocks.WATER.defaultBlockState()));
                this.duck.ate();
            }
        }
    }
}