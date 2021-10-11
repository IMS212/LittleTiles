package com.creativemd.littletiles.common.structure.type;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.spongepowered.asm.mixin.MixinEnvironment.Side;

import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.utils.math.RotationUtils;
import com.creativemd.littletiles.client.gui.controls.GuiDirectionIndicator;
import com.creativemd.littletiles.client.gui.controls.GuiTileViewer;
import com.creativemd.littletiles.common.action.block.LittleActionActivated;
import com.creativemd.littletiles.common.structure.animation.AnimationGuiHandler;
import com.creativemd.littletiles.common.structure.type.door.LittleSlidingDoor.LittleSlidingDoorParser;
import com.creativemd.littletiles.common.tile.parent.IStructureTileList;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.mojang.blaze3d.platform.GlStateManager;
import com.n247s.api.eventapi.eventsystem.CustomEventSubscribe;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayer.SleepResult;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.SideOnly;
import team.creative.creativecore.common.gui.GuiParent;
import team.creative.creativecore.common.gui.controls.simple.GuiStateButton;
import team.creative.creativecore.common.gui.event.GuiControlClickEvent;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.littletiles.LittleTiles;
import team.creative.littletiles.common.action.LittleActionException;
import team.creative.littletiles.common.math.vec.LittleVec;
import team.creative.littletiles.common.packet.LittleBedPacket;
import team.creative.littletiles.common.structure.LittleStructure;
import team.creative.littletiles.common.structure.LittleStructureType;
import team.creative.littletiles.common.structure.directional.StructureDirectional;
import team.creative.littletiles.common.structure.directional.StructureDirectionalField;
import team.creative.littletiles.common.structure.exception.CorruptedConnectionException;
import team.creative.littletiles.common.structure.registry.LittleStructureGuiParser;
import team.creative.littletiles.common.structure.registry.LittleStructureRegistry;
import team.creative.littletiles.common.tile.LittleTile;

public class LittleBed extends LittleStructure {
    
    public LittleBed(LittleStructureType type, IStructureTileList mainBlock) {
        super(type, mainBlock);
    }
    
    private EntityPlayer sleepingPlayer = null;
    @SideOnly(Side.CLIENT)
    public Vec3d playerPostion;
    @StructureDirectional
    public EnumFacing direction;
    
    @SideOnly(Side.CLIENT)
    public boolean hasBeenActivated;
    
    @Override
    protected void loadFromNBTExtra(NBTTagCompound nbt) {
        
    }
    
    @Override
    protected void writeToNBTExtra(NBTTagCompound nbt) {
        
    }
    
    @Override
    protected Object failedLoadingRelative(NBTTagCompound nbt, StructureDirectionalField field) {
        if (field.key.equals("facing"))
            return EnumFacing.getHorizontal(nbt.getInteger("direction"));
        return super.failedLoadingRelative(nbt, field);
    }
    
    @Override
    public boolean isBed(EntityLivingBase player) {
        return true;
    }
    
    public EntityPlayer getSleepingPlayer() {
        return sleepingPlayer;
    }
    
    public void setSleepingPlayer(EntityPlayer player) {
        this.sleepingPlayer = player;
        if (!getWorld().isRemote)
            getInput(0).updateState(BooleanUtils.asArray(player != null));
    }
    
    @SideOnly(Side.CLIENT)
    public static float getBedOrientationInDegrees(EntityPlayer player) {
        try {
            LittleStructure bed = (LittleStructure) littleBed.get(player);
            if (bed instanceof LittleBed)
                switch (((LittleBed) bed).direction) {
                case SOUTH:
                    return 90.0F;
                case WEST:
                    return 0.0F;
                case NORTH:
                    return 270.0F;
                case EAST:
                    return 180.0F;
                default:
                    return 0;
                }
            
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
        
        IBlockState state = player.bedLocation == null ? null : player.world.getBlockState(player.bedLocation);
        if (state != null && state.getBlock().isBed(state, player.world, player.bedLocation, player)) {
            EnumFacing enumfacing = state.getBlock().getBedDirection(state, player.world, player.bedLocation);
            
            switch (enumfacing) {
            case SOUTH:
                return 90.0F;
            case WEST:
                return 0.0F;
            case NORTH:
                return 270.0F;
            case EAST:
                return 180.0F;
            }
        }
        
        return 0.0F;
    }
    
    public static Method setSize = ReflectionHelper.findMethod(Entity.class, "setSize", "func_70105_a", float.class, float.class);
    
    public static Field sleeping = ReflectionHelper.findField(EntityPlayer.class, new String[] { "sleeping", "field_71083_bS" });
    public static Field sleepTimer = ReflectionHelper.findField(EntityPlayer.class, new String[] { "sleepTimer", "field_71076_b" });
    
    public static Field littleBed = ReflectionHelper.findField(EntityPlayer.class, "littleBed");;
    
    public SleepResult trySleep(EntityPlayer player, Vec3d highest) {
        if (!player.world.isRemote) {
            if (player.isPlayerSleeping() || !player.isEntityAlive()) {
                return EntityPlayer.SleepResult.OTHER_PROBLEM;
            }
            
            if (!player.world.provider.isSurfaceWorld()) {
                return EntityPlayer.SleepResult.NOT_POSSIBLE_HERE;
            }
            
            if (player.world.isDaytime()) {
                return EntityPlayer.SleepResult.NOT_POSSIBLE_NOW;
            }
            
            double d0 = 8.0D;
            double d1 = 5.0D;
            List<EntityMob> list = player.world
                    .<EntityMob>getEntitiesWithinAABB(EntityMob.class, new AxisAlignedBB(highest.x - 8.0D, highest.y - 5.0D, highest.z - 8.0D, highest.x + 8.0D, highest.y + 5.0D, highest.z + 8.0D));
            
            if (!list.isEmpty()) {
                return EntityPlayer.SleepResult.NOT_SAFE;
            }
        }
        
        if (player.isRiding()) {
            player.dismountRidingEntity();
        }
        if (player.world.isRemote)
            playerPostion = highest;
        setSleepingPlayer(player);
        
        try {
            LittleBed.littleBed.set(player, this);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
        
        try {
            setSize.invoke(player, 0.2F, 0.2F);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
        }
        
        float f1 = 0.5F + direction.getFrontOffsetX() * 0.8F;
        float f = 0.5F + direction.getFrontOffsetZ() * 0.8F;
        
        player.renderOffsetX = -1.8F * direction.getFrontOffsetX();
        player.renderOffsetZ = -1.8F * direction.getFrontOffsetZ();
        player.setPosition((float) highest.x - 0.5F + f1, ((float) highest.y), (float) highest.z - 0.5F + f);
        
        try {
            sleeping.setBoolean(player, true);
            sleepTimer.setInt(player, 0);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
        
        player.bedLocation = mainBlock.getPos();
        player.motionX = 0.0D;
        player.motionY = 0.0D;
        player.motionZ = 0.0D;
        
        if (!player.world.isRemote) {
            player.world.updateAllPlayersSleepingFlag();
        }
        return EntityPlayer.SleepResult.OK;
    }
    
    @Override
    public void onLittleTileDestroy() throws CorruptedConnectionException, NotYetConnectedException {
        super.onLittleTileDestroy();
        if (sleepingPlayer != null) {
            try {
                littleBed.set(sleepingPlayer, null);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
    
    @SideOnly(Side.CLIENT)
    public static void setBedDirection(Entity player) {
        if (player instanceof EntityPlayer) {
            try {
                LittleStructure bed = (LittleStructure) littleBed.get(player);
                if (bed instanceof LittleBed) {
                    int i = ((LittleBed) bed).direction.getHorizontalIndex();
                    
                    GlStateManager.rotate(i * 90, 0.0F, 1.0F, 0.0F);
                    
                    if (player == Minecraft.getMinecraft().player) {
                        double height = 0.2;
                        double forward = 0;
                        
                        GlStateManager
                                .translate(((LittleBed) bed).direction.getDirectionVec().getX() * forward, height, ((LittleBed) bed).direction.getDirectionVec().getZ() * forward);
                    }
                    // GlStateManager.translate(0, ((LittleBed) bed).playerPostion.getPosY() -
                    // player.posY, 0);
                    
                    // Minecraft.getMinecraft().getRenderManager().playerViewY =
                    // (float)(((LittleBed) bed).direction.getHorizontalAngle() * 90 + 180);
                    // Minecraft.getMinecraft().getRenderManager().playerViewX = 0.0F;
                    
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public boolean onBlockActivated(World world, LittleTile tile, BlockPos pos, EntityPlayer player, EnumHand hand, ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ, LittleActionActivated action) throws LittleActionException {
        load();
        
        if (!LittleTiles.CONFIG.general.enableBed)
            return false;
        
        if (world.isRemote) {
            hasBeenActivated = true;
            return true;
        }
        if (world.provider.canRespawnHere() && world.getBiome(pos) != Biomes.HELL) {
            Vec3d vec = getHighestCenterVec();
            if (this.sleepingPlayer != null) {
                player.sendStatusMessage(new TextComponentTranslation("tile.bed.occupied", new Object[0]), true);
                return true;
            }
            
            SleepResult enumstatus = trySleep(player, vec);
            if (enumstatus == SleepResult.OK) {
                player.addStat(StatList.SLEEP_IN_BED);
                PacketHandler.sendPacketToPlayer(new LittleBedPacket(getStructureLocation()), (EntityPlayerMP) player);
                PacketHandler.sendPacketToTrackingPlayers(new LittleBedPacket(getStructureLocation(), player), (EntityPlayerMP) player);
                return true;
            } else {
                if (enumstatus == SleepResult.NOT_POSSIBLE_NOW) {
                    player.sendStatusMessage(new TextComponentTranslation("tile.bed.noSleep"), true);
                } else if (enumstatus == SleepResult.NOT_SAFE) {
                    player.sendStatusMessage(new TextComponentTranslation("tile.bed.notSafe"), true);
                }
                
                return true;
            }
        }
        return true;
    }
    
    public static class LittleBedParser extends LittleStructureGuiParser {
        
        public LittleBedParser(GuiParent parent, AnimationGuiHandler handler) {
            super(parent, handler);
        }
        
        @SideOnly(Side.CLIENT)
        @CustomEventSubscribe
        public void buttonClicked(GuiControlClickEvent event) {
            GuiTileViewer viewer = (GuiTileViewer) parent.get("tileviewer");
            GuiDirectionIndicator relativeDirection = (GuiDirectionIndicator) parent.get("relativeDirection");
            
            EnumFacing direction = EnumFacing.getHorizontal(((GuiStateButton) parent.get("direction")).getState());
            
            LittleSlidingDoorParser.updateDirection(viewer, direction.getOpposite(), relativeDirection);
        }
        
        @Override
        @SideOnly(Side.CLIENT)
        public void createControls(LittlePreviews previews, LittleStructure structure) {
            GuiTileViewer tile = new GuiTileViewer("tileviewer", 0, 0, 100, 100, previews.getContext());
            tile.setViewDirection(EnumFacing.UP);
            parent.addControl(tile);
            
            LittleVec size = previews.getSize();
            int index = EnumFacing.EAST.getHorizontalIndex();
            if (size.x < size.z)
                index = EnumFacing.SOUTH.getHorizontalIndex();
            if (structure instanceof LittleBed)
                index = ((LittleBed) structure).direction.getHorizontalIndex();
            if (index < 0)
                index = 0;
            parent.addControl(new GuiStateButton("direction", index, 110, 0, 37, RotationUtils.getHorizontalFacingNames()));
            
            GuiDirectionIndicator relativeDirection = new GuiDirectionIndicator("relativeDirection", 155, 0, EnumFacing.UP);
            parent.addControl(relativeDirection);
            LittleSlidingDoorParser.updateDirection(tile, EnumFacing.getHorizontal(index).getOpposite(), relativeDirection);
        }
        
        @Override
        @SideOnly(Side.CLIENT)
        public LittleBed parseStructure(LittlePreviews previews) {
            EnumFacing direction = EnumFacing.getHorizontal(((GuiStateButton) parent.get("direction")).getState());
            LittleBed bed = createStructure(LittleBed.class, null);
            bed.direction = direction;
            return bed;
        }
        
        @Override
        @SideOnly(Side.CLIENT)
        protected LittleStructureType getStructureType() {
            return LittleStructureRegistry.getStructureType(LittleBed.class);
        }
    }
    
}
