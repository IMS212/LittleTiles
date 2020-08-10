package com.creativemd.littletiles.common.util.converation;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.creativemd.littletiles.common.block.BlockTile;
import com.creativemd.littletiles.common.mod.chiselsandbits.ChiselsAndBitsManager;
import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;

public class ChiselAndBitsConveration {
	
	public static ConcurrentLinkedQueue<TileEntity> tileentities = new ConcurrentLinkedQueue<>();
	
	@SubscribeEvent
	public static void worldTick(WorldTickEvent event) {
		World world = event.world;
		if (!world.isRemote && event.phase == Phase.END) {
			LittleGridContext chiselContext = LittleGridContext.get(ChiselsAndBitsManager.convertingFrom);
			int progress = 0;
			int size = tileentities.size();
			if (!tileentities.isEmpty())
				System.out.println("Attempting to convert " + size + " blocks ...");
			while (!tileentities.isEmpty()) {
				TileEntity te = tileentities.poll();
				List<LittleTile> tiles = ChiselsAndBitsManager.getTiles(te);
				if (tiles != null && tiles.size() > 0) {
					te.getWorld().setBlockState(te.getPos(), BlockTile.getState(false, false));
					TileEntityLittleTiles tileEntity = (TileEntityLittleTiles) te.getWorld().getTileEntity(te.getPos());
					tileEntity.convertTo(chiselContext);
					tileEntity.updateTiles((x) -> {
						x.noneStructureTiles().addAll(tiles);
					});
					
				}
				progress++;
				if (progress % 100 == 0)
					System.out.println("Converted " + progress + "/" + size + " blocks ...");
			}
			if (size > 0)
				System.out.println("Converted " + size + " blocks ...");
		}
	}
	
	public static void onAddedTileEntity(TileEntity te) {
		if (ChiselsAndBitsManager.isInstalled() && ChiselsAndBitsManager.isChiselsAndBitsStructure(te))
			tileentities.add(te);
	}
	
}
