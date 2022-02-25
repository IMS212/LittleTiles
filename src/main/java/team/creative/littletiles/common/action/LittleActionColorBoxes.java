package team.creative.littletiles.common.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Consumer;

import com.creativemd.littletiles.common.tile.LittleTileColored;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import team.creative.creativecore.common.util.filter.BiFilter;
import team.creative.creativecore.common.util.math.base.Axis;
import team.creative.creativecore.common.util.mc.ColorUtils;
import team.creative.creativecore.common.util.type.map.HashMapList;
import team.creative.littletiles.LittleTiles;
import team.creative.littletiles.common.block.entity.BETiles;
import team.creative.littletiles.common.block.entity.BETiles.BlockEntityInteractor;
import team.creative.littletiles.common.block.little.tile.LittleTile;
import team.creative.littletiles.common.block.little.tile.parent.IParentCollection;
import team.creative.littletiles.common.config.LittleTilesConfig.NotAllowedToPlaceColorException;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.ingredient.BlockIngredientEntry;
import team.creative.littletiles.common.ingredient.ColorIngredient;
import team.creative.littletiles.common.ingredient.LittleIngredients;
import team.creative.littletiles.common.ingredient.LittleInventory;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.LittleBoxAbsolute;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
import team.creative.littletiles.common.math.box.collection.LittleBoxesSimple;
import team.creative.littletiles.common.math.box.volume.LittleBoxReturnedVolume;
import team.creative.littletiles.common.structure.LittleStructure;
import team.creative.littletiles.common.structure.exception.CorruptedConnectionException;
import team.creative.littletiles.common.structure.exception.NotYetConnectedException;

public class LittleActionColorBoxes extends LittleActionBoxes {
    
    public int color;
    public boolean toVanilla;
    
    public LittleActionColorBoxes(Level level, LittleBoxes boxes, int color, boolean toVanilla) {
        super(level, boxes);
        this.color = color;
        this.toVanilla = toVanilla;
    }
    
    public LittleActionColorBoxes(UUID levelUUID, LittleBoxes boxes, int color, boolean toVanilla) {
        super(levelUUID, boxes);
        this.color = color;
        this.toVanilla = toVanilla;
    }
    
    public LittleActionColorBoxes() {
        
    }
    
    public transient HashMapList<Integer, LittleBoxes> revertList;
    
    public void addRevert(int color, BlockPos pos, LittleGrid grid, List<LittleBox> boxes) {
        LittleBoxes newBoxes = new LittleBoxesSimple(pos, grid);
        for (LittleBox box : boxes)
            newBoxes.add(box.copy());
        revertList.add(color, newBoxes);
    }
    
    public boolean shouldSkipTile(IParentCollection parent, LittleTile tile) {
        return false;
    }
    
    public transient boolean doneSomething;
    
    private transient double colorVolume;
    
    public ColorIngredient action(BETiles be, List<LittleBox> boxes, ColorIngredient gained, boolean simulate, LittleGrid grid) {
        doneSomething = false;
        colorVolume = 0;
        
        Consumer<BlockEntityInteractor> consumer = x -> {
            structure_loop: for (IParentCollection parent : be.groups()) {
                
                for (LittleTile tile : parent) {
                    
                    if (shouldSkipTile(parent, tile))
                        continue;
                    
                    LittleBox intersecting = null;
                    boolean intersects = false;
                    for (int j = 0; j < boxes.size(); j++) {
                        if (tile.intersectsWith(boxes.get(j))) {
                            intersects = true;
                            intersecting = boxes.get(j);
                            break;
                        }
                    }
                    
                    if (!intersects)
                        continue;
                    
                    try {
                        if (parent.isStructure() && parent.getStructure().hasStructureColor()) {
                            LittleStructure structure = parent.getStructure();
                            if (structure.getStructureColor() != color) {
                                double volume = structure.getPercentVolume();
                                colorVolume += volume;
                                gained.add(ColorIngredient.getColors(color, structure.getDefaultColor(), volume));
                                if (!simulate) {
                                    addRevert(structure.getStructureColor(), be.getBlockPos(), grid, Arrays.asList(intersecting));
                                    structure.paint(color);
                                }
                            }
                            continue structure_loop;
                        }
                    } catch (CorruptedConnectionException | NotYetConnectedException e) {
                        continue structure_loop;
                    }
                    
                    if (tile.color == color)
                        continue;
                    
                    doneSomething = true;
                    
                    if (!tile.equalsBox(intersecting)) {
                        if (simulate) {
                            double volume = 0;
                            List<LittleBox> cutout = new ArrayList<>();
                            LittleBoxReturnedVolume returnedVolume = new LittleBoxReturnedVolume();
                            tile.cutOut(boxes, cutout, returnedVolume);
                            for (LittleBox box2 : cutout) {
                                colorVolume += box2.getPercentVolume(grid);
                                volume += box2.getPercentVolume(grid);
                            }
                            if (returnedVolume.has()) {
                                colorVolume += returnedVolume.getPercentVolume(grid);
                                volume += returnedVolume.getPercentVolume(grid);
                            }
                            
                            gained.add(ColorIngredient.getColors(tile.getPreviewTile(), volume));
                            
                        } else {
                            List<LittleBox> cutout = new ArrayList<>();
                            List<LittleBox> newBoxes = tile.cutOut(boxes, cutout, null);
                            
                            if (newBoxes != null) {
                                addRevert(LittleTileColored.getColor(tile), be.getBlockPos(), grid, cutout);
                                
                                LittleTile tempTile = tile.copy();
                                LittleTile changedTile = LittleTileColored.setColor(tempTile, color);
                                if (changedTile == null)
                                    changedTile = tempTile;
                                
                                for (int i = 0; i < newBoxes.size(); i++) {
                                    LittleTile newTile = tile.copy();
                                    newTile.setBox(newBoxes.get(i));
                                    x.get(parent).add(newTile);
                                }
                                
                                for (int i = 0; i < cutout.size(); i++) {
                                    LittleTile newTile = changedTile.copy();
                                    newTile.setBox(cutout.get(i));
                                    x.get(parent).add(newTile);
                                }
                                
                                x.get(parent).remove(tile);
                            }
                        }
                    } else {
                        if (simulate) {
                            colorVolume += tile.getPercentVolume(grid);
                            gained.add(ColorIngredient.getColors(tile.getPreviewTile(), tile.getPercentVolume(grid)));
                        } else {
                            List<LittleBox> oldBoxes = new ArrayList<>();
                            oldBoxes.add(tile.getBox());
                            
                            addRevert(LittleTileColored.getColor(tile), be.getBlockPos(), grid, oldBoxes);
                            
                            LittleTile changedTile = LittleTileColored.setColor(tile, color);
                            if (changedTile != null) {
                                x.get(parent).add(changedTile);
                                x.get(parent).remove(tile);
                            }
                        }
                    }
                }
            }
        };
        
        if (simulate)
            be.updateTilesSecretly(consumer);
        else
            be.updateTiles(consumer);
        
        ColorIngredient toDrain = ColorIngredient.getColors(color);
        toDrain.scale(colorVolume);
        
        return gained.sub(toDrain);
    }
    
    @Override
    public void action(Level level, Player player, BlockPos pos, BlockState state, List<LittleBox> boxes, LittleGrid grid) throws LittleActionException {
        if (ColorUtils.alpha(color) < LittleTiles.CONFIG.getMinimumTransparency(player))
            throw new NotAllowedToPlaceColorException(player);
        
        fireBlockBreakEvent(level, pos, player);
        
        BlockEntity blockEntity = loadBE(player, level, pos, null, true, 0);
        
        if (blockEntity instanceof BETiles) {
            BETiles be = (BETiles) blockEntity;
            
            if (grid != be.getGrid()) {
                if (grid.count < be.getGrid().count) {
                    for (LittleBox box : boxes)
                        box.convertTo(grid, be.getGrid());
                    grid = be.getGrid();
                } else
                    be.convertTo(grid);
            }
            
            List<BlockIngredientEntry> entries = new ArrayList<>();
            
            ColorIngredient gained = new ColorIngredient();
            
            ColorIngredient toDrain = action(be, boxes, gained, true, grid);
            LittleIngredients gainedIngredients = new LittleIngredients(gained);
            LittleIngredients drainedIngredients = new LittleIngredients(toDrain);
            LittleInventory inventory = new LittleInventory(player);
            try {
                inventory.startSimulation();
                give(player, inventory, gainedIngredients);
                take(player, inventory, drainedIngredients);
            } finally {
                inventory.stopSimulation();
            }
            
            give(player, inventory, gainedIngredients);
            take(player, inventory, drainedIngredients);
            action(be, boxes, gained, false, grid);
            
            be.combineTiles();
            
            if (toVanilla || !doneSomething)
                be.convertBlockToVanilla();
        }
    }
    
    @Override
    public boolean action(Player player) throws LittleActionException {
        revertList = new HashMapList<>();
        return super.action(player);
    }
    
    @Override
    public boolean canBeReverted() {
        return true;
    }
    
    @Override
    public LittleAction revert(Player player) {
        List<LittleAction> actions = new ArrayList<>();
        for (Entry<Integer, ArrayList<LittleBoxes>> entry : revertList.entrySet()) {
            for (LittleBoxes boxes : entry.getValue()) {
                boxes.convertToSmallest();
                actions.add(new LittleActionColorBoxes(levelUUID, boxes, entry.getKey(), true));
            }
        }
        return new LittleActions(actions.toArray(new LittleAction[0]));
    }
    
    @Override
    public LittleAction mirror(Axis axis, LittleBoxAbsolute box) {
        LittleActionColorBoxes action = new LittleActionColorBoxes();
        action.color = color;
        action.toVanilla = toVanilla;
        return assignFlip(action, axis, box);
    }
    
    public static class LittleActionColorBoxesFiltered extends LittleActionColorBoxes {
        
        public BiFilter<IParentCollection, LittleTile> filter;
        
        public LittleActionColorBoxesFiltered(Level level, LittleBoxes boxes, int color, boolean toVanilla, BiFilter<IParentCollection, LittleTile> filter) {
            super(level, boxes, color, toVanilla);
            this.filter = filter;
        }
        
        public LittleActionColorBoxesFiltered() {
            
        }
        
        @Override
        public boolean shouldSkipTile(IParentCollection parent, LittleTile tile) {
            return !filter.is(parent, tile);
        }
        
        @Override
        public LittleAction mirror(Axis axis, LittleBoxAbsolute box) {
            LittleActionColorBoxesFiltered action = new LittleActionColorBoxesFiltered();
            action.filter = filter;
            action.color = color;
            action.toVanilla = toVanilla;
            return assignFlip(action, axis, box);
        }
    }
}
