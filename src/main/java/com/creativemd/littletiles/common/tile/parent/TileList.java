package com.creativemd.littletiles.common.tile.parent;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;

import com.creativemd.creativecore.common.utils.type.Pair;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.attribute.LittleStructureAttribute;
import com.creativemd.littletiles.common.structure.exception.CorruptedConnectionException;
import com.creativemd.littletiles.common.structure.exception.NotYetConnectedException;
import com.creativemd.littletiles.common.tile.LittleTile;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class TileList extends ParentTileList {
	
	public final TileEntityLittleTiles te;
	
	private final HashMap<Integer, StructureTileList> structures = new HashMap<>();
	private int attributes = LittleStructureAttribute.NONE;
	
	private final boolean client;
	
	public TileList(TileEntityLittleTiles te, boolean client) {
		super();
		this.te = te;
		this.client = client;
	}
	
	@Override
	protected void readExtra(NBTTagCompound nbt) {
		structures.clear();
		NBTTagList list = nbt.getTagList("children", 10);
		for (int i = 0; i < list.tagCount(); i++) {
			StructureTileList child = new StructureTileList(this, list.getCompoundTagAt(i));
			structures.put(child.getIndex(), child);
		}
	}
	
	@Override
	protected void writeExtra(NBTTagCompound nbt) {
		NBTTagList list = new NBTTagList();
		for (StructureTileList child : structures.values())
			list.appendTag(child.write());
		nbt.setTag("children", list);
	}
	
	public void clearEverything() {
		super.clear();
		structures.clear();
	}
	
	public int countStructures() {
		return structures.size();
	}
	
	public boolean removeStructure(int index) {
		boolean removed = structures.remove(index) != null;
		reloadAttributes();
		return removed;
	}
	
	public void addStructure(int index, StructureTileList list) {
		if (structures.containsKey(index))
			throw new IllegalArgumentException("index '" + index + "' already exists");
		structures.put(index, list);
	}
	
	public StructureTileList addStructure(int index, int attribute) {
		if (structures.containsKey(index))
			throw new IllegalArgumentException("index '" + index + "' already exists");
		StructureTileList list = new StructureTileList(this, index, attribute);
		structures.put(index, list);
		reloadAttributes();
		return list;
	}
	
	public Iterable<LittleStructure> loadedStructures() {
		return new Iterable<LittleStructure>() {
			
			@Override
			public Iterator<LittleStructure> iterator() {
				return new Iterator<LittleStructure>() {
					
					Iterator<StructureTileList> itr = structures.values().iterator();
					
					@Override
					public boolean hasNext() {
						return itr.hasNext();
					}
					
					@Override
					public LittleStructure next() {
						try {
							return itr.next().getStructure();
						} catch (CorruptedConnectionException | NotYetConnectedException e) {
							throw new RuntimeException(e);
						}
					}
				};
			}
		};
	}
	
	public Iterable<IStructureTileList> structures() {
		return new Iterable<IStructureTileList>() {
			
			@Override
			public Iterator<IStructureTileList> iterator() {
				return new Iterator<IStructureTileList>() {
					
					Iterator<StructureTileList> iterator = structures.values().iterator();
					
					@Override
					public boolean hasNext() {
						return iterator.hasNext();
					}
					
					@Override
					public IStructureTileList next() {
						return iterator.next();
					}
				};
			}
		};
	}
	
	public Iterable<StructureTileList> structuresReal() {
		return structures.values();
	}
	
	public Iterable<LittleStructure> loadedStructures(int attribute) {
		return new Iterable<LittleStructure>() {
			
			@Override
			public Iterator<LittleStructure> iterator() {
				if (LittleStructureAttribute.listener(attribute) || LittleStructureAttribute.active(attribute)) {
					boolean active = LittleStructureAttribute.active(attribute);
					return new Iterator<LittleStructure>() {
						
						public Iterator<StructureTileList> iterator = structures.values().iterator();
						public LittleStructure next;
						
						{
							findNext();
						}
						
						public void findNext() {
							while (iterator.hasNext()) {
								StructureTileList structure = iterator.next();
								if ((!active || structure.isMain()) && (structure.getAttribute() & attribute) != 0) {
									try {
										next = structure.getStructure();
									} catch (CorruptedConnectionException | NotYetConnectedException e) {
									
									}
									return;
								}
							}
							
							next = null;
						}
						
						@Override
						public boolean hasNext() {
							return next != null;
						}
						
						@Override
						public LittleStructure next() {
							LittleStructure toReturn = next;
							findNext();
							return toReturn;
						}
					};
				}
				return new Iterator<LittleStructure>() {
					
					@Override
					public boolean hasNext() {
						return false;
					}
					
					@Override
					public LittleStructure next() {
						return null;
					}
				};
			}
		};
	}
	
	public boolean hasTicking() {
		return LittleStructureAttribute.ticking(attributes);
	}
	
	public boolean hasRendered() {
		return LittleStructureAttribute.tickRendering(attributes);
	}
	
	public boolean hasCollisionListener() {
		if (checkCollision() && LittleStructureAttribute.collisionListener(attributes))
			return true;
		for (StructureTileList child : structures.values())
			if (child.checkCollision())
				return true;
		return false;
	}
	
	public Iterable<Pair<IParentTileList, LittleTile>> allTiles() {
		return new Iterable<Pair<IParentTileList, LittleTile>>() {
			
			@Override
			public Iterator<Pair<IParentTileList, LittleTile>> iterator() {
				return new Iterator<Pair<IParentTileList, LittleTile>>() {
					
					Iterator<LittleTile> current = TileList.this.iterator();
					Pair<IParentTileList, LittleTile> pair = new Pair<>(TileList.this, null);
					Iterator<StructureTileList> overall = structures.values().iterator();
					
					@Override
					public boolean hasNext() {
						inc();
						return current.hasNext();
					}
					
					void inc() {
						while (!current.hasNext())
							if (overall.hasNext()) {
								StructureTileList list = overall.next();
								pair = new Pair(list, null);
								current = list.iterator();
							} else
								return;
					}
					
					@Override
					public Pair<IParentTileList, LittleTile> next() {
						pair.setValue(current.next());
						return pair;
					}
				};
			}
		};
	}
	
	@Override
	public LittleTile first() {
		return isEmpty() ? null : super.get(0);
	}
	
	@Override
	public LittleTile last() {
		return isEmpty() ? null : super.get(size() - 1);
	}
	
	private void reloadAttributes() {
		attributes = LittleStructureAttribute.NONE;
		for (StructureTileList structure : structures.values())
			attributes |= structure.getAttribute();
	}
	
	@Override
	public TileEntityLittleTiles getTe() {
		return te;
	}
	
	@Override
	public boolean isStructure() {
		return false;
	}
	
	@Override
	public boolean isMain() {
		return false;
	}
	
	@Override
	public boolean isStructureChild(LittleStructure structure) throws CorruptedConnectionException, NotYetConnectedException {
		return false;
	}
	
	@Override
	public LittleStructure getStructure() {
		return null;
	}
	
	public StructureTileList getStructure(int index) {
		return structures.get(index);
	}
	
	@Override
	public int getAttribute() {
		return 0;
	}
	
	@Override
	public boolean isClient() {
		return client;
	}
	
	public void add(TileList tiles) {
		addAll(tiles);
		tiles.structures.putAll(structures);
		for (StructureTileList list : structures.values()) {
			list.setParent(this);
		}
	}
	
	public void fillUsedIds(BitSet usedIds) {
		for (Integer id : structures.keySet())
			usedIds.set(id);
	}
	
	public void removeEmptyLists() {
		for (Iterator<StructureTileList> iterator = structures.values().iterator(); iterator.hasNext();) {
			StructureTileList child = iterator.next();
			if (child.isEmpty())
				iterator.remove();
		}
	}
	
}