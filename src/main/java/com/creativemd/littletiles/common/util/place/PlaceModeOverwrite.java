package com.creativemd.littletiles.common.util.place;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.creativemd.littletiles.common.action.block.LittleActionDestroyBoxes;
import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tile.parent.IParentTileList;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.littletiles.common.util.place.Placement.PlacementBlock;

import net.minecraft.util.math.BlockPos;

public class PlaceModeOverwrite extends PlacementMode {
	
	public PlaceModeOverwrite(String name, PreviewMode mode) {
		super(name, mode, false);
	}
	
	@Override
	public boolean shouldConvertBlock() {
		return true;
	}
	
	@Override
	public boolean canPlaceStructures() {
		return true;
	}
	
	@Override
	public boolean checkAll() {
		return false;
	}
	
	@Override
	public List<BlockPos> getCoordsToCheck(Set<BlockPos> splittedTiles, BlockPos pos) {
		return new ArrayList<>(splittedTiles);
	}
	
	@Override
	public List<LittleTile> placeTile(Placement placement, PlacementBlock block, IParentTileList parent, LittleTile tile, boolean requiresCollisionTest) {
		List<LittleTile> tiles = new ArrayList<>();
		LittleGridContext context = block.getContext();
		if (requiresCollisionTest)
			for (LittleTile removedTile : LittleActionDestroyBoxes.removeBox(block.getTe(), context, tile.getBox(), false))
				placement.removedTiles.addTile(parent, removedTile);
		block.getTe().convertTo(context);
		tiles.add(tile);
		return tiles;
	}
	
}
