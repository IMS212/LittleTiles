package team.creative.littletiles.common.block.little;

import java.lang.reflect.Field;
import java.util.Random;

import com.creativemd.littletiles.client.render.tile.LittleRenderBox;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import team.creative.creativecore.common.util.math.base.Axis;
import team.creative.creativecore.common.util.math.transformation.Rotation;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.littletiles.common.api.block.LittleBlock;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.vec.LittleVec;
import team.creative.littletiles.common.tile.LittleTile;
import team.creative.littletiles.common.tile.parent.IParentCollection;

public class LittleMCBlock implements LittleBlock {
    
    public static final Field hasCollisionField = ObfuscationReflectionHelper.findField(BlockBehaviour.class, "f_60443_");
    
    public final Block block;
    private final boolean translucent;
    
    public LittleMCBlock(Block block) {
        this.block = block;
        this.translucent = !block.defaultBlockState().getMaterial().isSolid() || !block.defaultBlockState().getMaterial().isSolid() || block.defaultBlockState().canOcclude(); // Also depends on block model
    }
    
    @Override
    public boolean is(ItemStack stack) {
        return Block.byItem(stack.getItem()) == block;
    }
    
    @Override
    public ItemStack getStack() {
        return new ItemStack(block);
    }
    
    @Override
    public boolean canBeRenderCombined(LittleTile one, LittleTile two) {
        return one.getBlock() == two.getBlock() && one.color == two.color;
    }
    
    @Override
    public boolean noCollision() {
        try {
            return !hasCollisionField.getBoolean(block);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    @Override
    public boolean is(Block block) {
        return this.block == block;
    }
    
    @Override
    public boolean isTranslucent() {
        return translucent;
    }
    
    @Override
    public boolean canBeConvertedToVanilla() {
        return true;
    }
    
    @Override
    public LittleBlock mirror(Axis axis, LittleVec doubledCenter) {
        return this;
    }
    
    @Override
    public LittleBlock rotate(Rotation rotation, LittleVec doubledCenter) {
        return this;
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public SoundType getSoundType() {
        return block.getSoundType(getState());
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public float getExplosionResistance(LittleTile tile) {
        return block.getExplosionResistance();
    }
    
    @Override
    public void exploded(IParentCollection parent, LittleTile tile, Explosion explosion) {}
    
    @Override
    public boolean randomTicks() {
        return false;
    }
    
    @Override
    public void randomDisplayTick(IParentCollection parent, LittleTile tile, Random rand) {}
    
    @Override
    @SuppressWarnings("deprecation")
    public int getLightValue() {
        return getState().getLightEmission();
    }
    
    @Override
    public BlockState getState() {
        return block.defaultBlockState();
    }
    
    @Override
    public String blockName() {
        return block.getRegistryName().toString();
    }
    
    @Override
    public boolean canInteract() {
        return false;
    }
    
    @Override
    public InteractionResult use(IParentCollection parent, LittleBox box, Player player, InteractionHand hand, BlockHitResult result) {
        return InteractionResult.PASS;
    }
    
    @Override
    public float getEnchantPowerBonus(IParentCollection parent, LittleTile tile) {
        float bonus = block.getEnchantPowerBonus(getState(), parent.getLevel(), parent.getPos());
        if (bonus > 0)
            return (float) (bonus * tile.getPercentVolume(parent.getGrid()));
        return 0;
    }
    
    @Override
    public float getFriction(IParentCollection parent, LittleTile tile, Entity entity) {
        return block.getFriction(getState(), parent.getLevel(), parent.getPos(), entity);
    }
    
    @Override
    public boolean isMaterial(Material material) {
        return getState().getMaterial() == material;
    }
    
    @Override
    public boolean isLiquid() {
        return getState().getMaterial().isLiquid();
    }
    
    @Override
    public LittleRenderBox getRenderBox(LittleGrid grid, RenderType layer, LittleBox box, int color) {
        return new LittleRenderBox(box.getBox(grid), box, block, color);
    }
    
    @Override
    public boolean checkEntityCollision() {
        return false;
    }
    
    @Override
    public void entityCollided(IParentCollection parent, LittleTile tile, Entity entity) {}
    
    @Override
    public Vec3d getFogColor(IParentCollection parent, LittleTile tile, Entity entity, Vec3d originalColor, float partialTicks) {
        return originalColor;
    }
    
    @Override
    public Vec3d modifyAcceleration(IParentCollection parent, LittleTile tile, Entity entity, Vec3d motion) {
        return motion;
    }
}
