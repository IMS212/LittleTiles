package team.creative.littletiles.common.structure.type.door;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.MixinEnvironment.Side;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.controls.gui.GuiScrollBox;
import com.creativemd.creativecore.common.utils.type.UUIDSupplier;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.n247s.api.eventapi.eventsystem.CustomEventSubscribe;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fml.relauncher.SideOnly;
import team.creative.creativecore.common.gui.GuiParent;
import team.creative.creativecore.common.gui.controls.simple.GuiCheckBox;
import team.creative.creativecore.common.gui.event.GuiControlChangedEvent;
import team.creative.creativecore.common.util.type.PairList;
import team.creative.littletiles.common.action.LittleActionException;
import team.creative.littletiles.common.animation.AnimationGuiHandler;
import team.creative.littletiles.common.animation.entity.EntityAnimation;
import team.creative.littletiles.common.animation.event.AnimationEvent;
import team.creative.littletiles.common.animation.event.ChildActivateEvent;
import team.creative.littletiles.common.animation.timeline.AnimationTimeline;
import team.creative.littletiles.common.block.little.tile.parent.IStructureParentCollection;
import team.creative.littletiles.common.structure.LittleStructure;
import team.creative.littletiles.common.structure.LittleStructureType;
import team.creative.littletiles.common.structure.exception.CorruptedConnectionException;
import team.creative.littletiles.common.structure.registry.LittleStructureGuiParser;
import team.creative.littletiles.common.structure.registry.LittleStructureRegistry;
import team.creative.littletiles.common.structure.type.door.LittleDoorBase.LittleDoorBaseType;

public class LittleDoorActivator extends LittleDoor {
    
    public int[] toActivate;
    
    public boolean inMotion = false;
    
    public LittleDoorActivator(LittleStructureType type, IStructureParentCollection mainBlock) {
        super(type, mainBlock);
    }
    
    @Override
    protected void saveExtra(CompoundTag nbt) {
        super.saveExtra(nbt);
        nbt.putIntArray("activate", toActivate);
        nbt.putBoolean("inMotion", inMotion);
    }
    
    @Override
    protected void loadExtra(CompoundTag nbt) {
        super.loadExtra(nbt);
        toActivate = nbt.getIntArray("activate");
        inMotion = nbt.getBoolean("inMotion");
    }
    
    public LittleDoor getChildrenDoor(int index) throws CorruptedConnectionException, NotYetConnectedException {
        if (index >= 0 && index < countChildren()) {
            LittleStructure structure = getChild(index).getStructure();
            if (structure instanceof LittleDoor)
                return (LittleDoor) structure;
            return null;
        }
        return null;
    }
    
    @Override
    public EntityAnimation openDoor(@Nullable EntityPlayer player, UUIDSupplier uuid, boolean tickOnce) throws LittleActionException {
        inMotion = true;
        for (int i : toActivate) {
            LittleDoor child = getChildrenDoor(i);
            if (child == null)
                continue;
            EntityAnimation childAnimation = child.openDoor(player, uuid, tickOnce);
            if (childAnimation != null)
                childAnimation.controller.onServerApproves();
        }
        return null;
    }
    
    @Override
    public int getCompleteDuration() {
        int duration = 0;
        for (int i : toActivate) {
            
            try {
                LittleDoor child;
                child = getChildrenDoor(i);
                if (child == null)
                    continue;
                duration = Math.max(duration, child.getCompleteDuration());
            } catch (CorruptedConnectionException | NotYetConnectedException e) {}
        }
        return duration;
    }
    
    @Override
    public List<LittleDoor> collectDoorsToCheck() {
        List<LittleDoor> doors = new ArrayList<>();
        for (int i : toActivate) {
            try {
                LittleDoor child = getChildrenDoor(i);
                if (child == null)
                    continue;
                doors.add(child);
            } catch (CorruptedConnectionException | NotYetConnectedException e) {}
        }
        return doors;
    }
    
    @Override
    public boolean isInMotion() {
        return inMotion;
    }
    
    public boolean checkChildrenInMotion() {
        for (int i : toActivate) {
            try {
                LittleDoor child = getChildrenDoor(i);
                if (child == null)
                    continue;
                if (child.isInMotion())
                    return true;
            } catch (CorruptedConnectionException | NotYetConnectedException e) {}
        }
        return false;
    }
    
    @Override
    public void onChildComplete(LittleDoor door, int childId) {
        inMotion = checkChildrenInMotion();
        if (!inMotion)
            completeAnimation();
    }
    
    public static class LittleDoorActivatorParser extends LittleStructureGuiParser {
        
        public LittleDoorActivatorParser(GuiParent parent, AnimationGuiHandler handler) {
            super(parent, handler);
        }
        
        public String getDisplayName(LittlePreviews previews, int childId) {
            String name = previews.getStructureName();
            if (name == null)
                if (previews.hasStructure())
                    name = previews.getStructureId();
                else
                    name = "none";
            return name + " " + childId;
        }
        
        public List<Integer> possibleChildren;
        
        @Override
        public void createControls(LittlePreviews previews, LittleStructure structure) {
            parent.controls.add(new GuiCheckBox("rightclick", CoreControl
                    .translate("gui.door.rightclick"), 50, 123, structure instanceof LittleDoor ? !((LittleDoor) structure).disableRightClick : true));
            
            GuiScrollBox box = new GuiScrollBox("content", 0, 0, 100, 115);
            parent.controls.add(box);
            LittleDoorActivator activator = structure instanceof LittleDoorActivator ? (LittleDoorActivator) structure : null;
            possibleChildren = new ArrayList<>();
            int i = 0;
            int added = 0;
            for (LittlePreviews child : previews.getChildren()) {
                Class clazz = LittleStructureRegistry.getStructureClass(child.getStructureId());
                if (clazz != null && LittleDoor.class.isAssignableFrom(clazz)) {
                    box.addControl(new GuiCheckBox("" + i, getDisplayName(child, i), 0, added * 20, activator != null && ArrayUtils.contains(activator.toActivate, i)));
                    possibleChildren.add(i);
                    added++;
                }
                i++;
            }
            
            updateTimeline();
        }
        
        @CustomEventSubscribe
        public void onChanged(GuiControlChangedEvent event) {
            if (event.source instanceof GuiCheckBox)
                updateTimeline();
        }
        
        public void updateTimeline() {
            AnimationTimeline timeline = new AnimationTimeline(0, new PairList<>());
            List<AnimationEvent> events = new ArrayList<>();
            for (Integer integer : possibleChildren) {
                GuiCheckBox box = (GuiCheckBox) parent.get("" + integer);
                if (box != null && box.value)
                    events.add(new ChildActivateEvent(0, integer));
            }
            handler.setTimeline(timeline, events);
        }
        
        @Override
        public LittleStructure parseStructure(LittlePreviews previews) {
            LittleDoorActivator activator = createStructure(LittleDoorActivator.class, null);
            
            GuiCheckBox rightclick = (GuiCheckBox) parent.get("rightclick");
            activator.disableRightClick = !rightclick.value;
            
            GuiScrollBox box = (GuiScrollBox) parent.get("content");
            List<Integer> toActivate = new ArrayList<>();
            for (Integer integer : possibleChildren) {
                GuiCheckBox checkBox = (GuiCheckBox) box.get("" + integer);
                if (checkBox != null && checkBox.value)
                    toActivate.add(integer);
            }
            activator.toActivate = new int[toActivate.size()];
            for (int i = 0; i < activator.toActivate.length; i++)
                activator.toActivate[i] = toActivate.get(i);
            
            return activator;
        }
        
        @Override
        @SideOnly(Side.CLIENT)
        protected LittleStructureType getStructureType() {
            return LittleStructureRegistry.getStructureType(LittleDoorActivator.class);
        }
        
    }
    
    public static class LittleDoorActivatorType extends LittleDoorBaseType {
        
        public LittleDoorActivatorType(String id, String category, Class<? extends LittleStructure> structureClass, int attribute) {
            super(id, category, structureClass, attribute);
        }
        
        @Override
        public void setBit(LittlePreviews previews, BitSet set) {
            for (int i : previews.structureNBT.getIntArray("activate"))
                set.set(i);
        }
    }
}
