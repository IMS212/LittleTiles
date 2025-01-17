package team.creative.littletiles.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelEvent.RegisterAdditional;
import net.minecraftforge.client.event.ModelEvent.RegisterGeometryLoaders;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import team.creative.creativecore.client.CreativeCoreClient;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.client.render.model.CreativeBlockModel;
import team.creative.creativecore.client.render.model.CreativeItemBoxModel;
import team.creative.creativecore.common.util.mc.ColorUtils;
import team.creative.littletiles.LittleTiles;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.client.action.LittleActionHandlerClient;
import team.creative.littletiles.client.action.interact.LittleInteractionHandlerClient;
import team.creative.littletiles.client.level.LevelHandlersClient;
import team.creative.littletiles.client.level.LittleAnimationHandlerClient;
import team.creative.littletiles.client.level.LittleVanillaInteractionHandlerClient;
import team.creative.littletiles.client.player.LittleClientPlayerConnection;
import team.creative.littletiles.client.render.block.BETilesRenderer;
import team.creative.littletiles.client.render.block.LittleBlockClientRegistry;
import team.creative.littletiles.client.render.cache.build.RenderingThread;
import team.creative.littletiles.client.render.entity.LittleEntityRenderer;
import team.creative.littletiles.client.render.entity.LittleSitRenderer;
import team.creative.littletiles.client.render.entity.RenderSizedTNTPrimed;
import team.creative.littletiles.client.render.item.ItemRenderCache;
import team.creative.littletiles.client.render.item.LittleModelItemBackground;
import team.creative.littletiles.client.render.item.LittleModelItemPreview;
import team.creative.littletiles.client.render.item.LittleModelItemTilesBig;
import team.creative.littletiles.client.render.level.LittleClientEventHandler;
import team.creative.littletiles.client.render.overlay.LittleTilesProfilerOverlay;
import team.creative.littletiles.client.render.overlay.OverlayRenderer;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.common.block.little.tile.group.LittleGroup;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.ingredient.BlockIngredientEntry;
import team.creative.littletiles.common.ingredient.ColorIngredient;
import team.creative.littletiles.common.item.ItemBlockIngredient;
import team.creative.littletiles.common.item.ItemColorIngredient;
import team.creative.littletiles.common.item.ItemLittleBlueprint;
import team.creative.littletiles.common.item.ItemLittleChisel;
import team.creative.littletiles.common.item.ItemLittleGlove;
import team.creative.littletiles.common.item.ItemLittlePaintBrush;
import team.creative.littletiles.common.item.ItemPremadeStructure;
import team.creative.littletiles.common.item.glove.GloveMode;
import team.creative.littletiles.common.placement.mode.PlacementMode;
import team.creative.littletiles.common.structure.type.premade.LittleStructurePremade.LittlePremadeType;

@OnlyIn(Dist.CLIENT)
public class LittleTilesClient {
    
    public static final Minecraft mc = Minecraft.getInstance();
    
    public static final IKeyConflictContext LITTLE_KEY_CONTEXT = new IKeyConflictContext() {
        
        @Override
        public boolean isActive() {
            return true;
        }
        
        @Override
        public boolean conflicts(IKeyConflictContext other) {
            return this == other;
        }
    };
    
    public static final LevelHandlersClient LEVEL_HANDLERS = new LevelHandlersClient();
    public static LittleActionHandlerClient ACTION_HANDLER;
    public static LittleAnimationHandlerClient ANIMATION_HANDLER;
    public static LittleVanillaInteractionHandlerClient INTERACTION_HANDLER;
    public static LittleInteractionHandlerClient INTERACTION;
    public static PreviewRenderer PREVIEW_RENDERER;
    public static ItemRenderCache ITEM_RENDER_CACHE;
    public static LittleClientPlayerConnection PLAYER_CONNECTION;
    public static OverlayRenderer OVERLAY_RENDERER;
    
    public static KeyMapping mirror;
    public static KeyMapping mark;
    public static KeyMapping configure;
    public static KeyMapping up;
    public static KeyMapping down;
    public static KeyMapping right;
    public static KeyMapping left;
    
    public static KeyMapping undo;
    public static KeyMapping redo;
    
    public static BETilesRenderer blockEntityRenderer;
    
    public static void grid(LittleGrid grid) {
        ACTION_HANDLER.setting.grid(grid);
    }
    
    public static void placementMode(PlacementMode mode) {
        ACTION_HANDLER.setting.placementMode(mode);
    }
    
    public static void setPlace(LittleGrid grid, PlacementMode mode) {
        ACTION_HANDLER.setting.set(grid, mode);
    }
    
    public static Component arrowKeysTooltip() {
        if (up.isDefault() && down.isDefault() && right.isDefault() && left.isDefault())
            return Component.translatable("gui.tooltip.arrow_keys");
        return Component.empty().append(up.getTranslatedKeyMessage()).append(", ").append(down.getTranslatedKeyMessage()).append(", ").append(right.getTranslatedKeyMessage())
                .append(", ").append(left.getTranslatedKeyMessage());
    }
    
    public static void displayActionMessage(List<Component> message) {
        OVERLAY_RENDERER.displayActionMessage(message);
    }
    
    public static void load(IEventBus bus) {
        bus.addListener(LittleTilesClient::setup);
        MinecraftForge.EVENT_BUS.addListener(LittleTilesClient::commands);
        bus.addListener(LittleTilesClient::initItemColors);
        bus.addListener(LittleTilesClient::initBlockColors);
        bus.addListener(LittleTilesClient::registerKeys);
        bus.addListener(LittleTilesClient::modelEvent);
        bus.addListener(LittleTilesClient::modelLoader);
    }
    
    private static void registerKeys(RegisterKeyMappingsEvent event) {
        up = new LittleKeyMapping("key.rotateup", LITTLE_KEY_CONTEXT, InputConstants.KEY_UP, "key.categories.littletiles").ignoreModifier();
        down = new LittleKeyMapping("key.rotatedown", LITTLE_KEY_CONTEXT, InputConstants.KEY_DOWN, "key.categories.littletiles").ignoreModifier();
        right = new LittleKeyMapping("key.rotateright", LITTLE_KEY_CONTEXT, InputConstants.KEY_RIGHT, "key.categories.littletiles").ignoreModifier();
        left = new LittleKeyMapping("key.rotateleft", LITTLE_KEY_CONTEXT, InputConstants.KEY_LEFT, "key.categories.littletiles").ignoreModifier();
        
        mirror = new LittleKeyMapping("key.little.mirror", LITTLE_KEY_CONTEXT, InputConstants.KEY_G, "key.categories.littletiles");
        mark = new LittleKeyMapping("key.little.mark", LITTLE_KEY_CONTEXT, InputConstants.KEY_M, "key.categories.littletiles");
        configure = new LittleKeyMapping("key.little.config.item", LITTLE_KEY_CONTEXT, InputConstants.KEY_C, "key.categories.littletiles");
        
        undo = new LittleKeyMapping("key.little.undo", LITTLE_KEY_CONTEXT, KeyModifier.CONTROL, InputConstants.KEY_Z, "key.categories.littletiles");
        redo = new LittleKeyMapping("key.little.redo", LITTLE_KEY_CONTEXT, KeyModifier.CONTROL, InputConstants.KEY_Y, "key.categories.littletiles");
        
        event.register(up);
        event.register(down);
        event.register(right);
        event.register(left);
        
        event.register(mirror);
        event.register(mark);
        event.register(configure);
        
        event.register(undo);
        event.register(redo);
    }
    
    private static void setup(final FMLClientSetupEvent event) {
        mc.getItemColors().register((stack, layer) -> {
            if (layer == 0)
                return ColorUtils.WHITE;
            return ItemLittlePaintBrush.getColor(stack);
        }, LittleTilesRegistry.PAINT_BRUSH.get());
        
        // overlay.add(new OverlayControl(new GuiAxisIndicatorControl("axis"), OverlayPositionType.CENTER).setShouldRender(() -> PreviewRenderer.marked != null));
        MinecraftForge.EVENT_BUS.register(new LittleClientEventHandler());
        
        LEVEL_HANDLERS.register(LittleActionHandlerClient::new, x -> ACTION_HANDLER = x);
        LEVEL_HANDLERS.register(LittleVanillaInteractionHandlerClient::new, x -> INTERACTION_HANDLER = x);
        LEVEL_HANDLERS.register(LittleTiles.ANIMATION_HANDLERS::get, x -> ANIMATION_HANDLER = (LittleAnimationHandlerClient) x);
        LEVEL_HANDLERS.register(PREVIEW_RENDERER = new PreviewRenderer());
        LEVEL_HANDLERS.register(ITEM_RENDER_CACHE = new ItemRenderCache());
        LEVEL_HANDLERS.register(PLAYER_CONNECTION = new LittleClientPlayerConnection());
        LEVEL_HANDLERS.register(INTERACTION = new LittleInteractionHandlerClient());
        
        // Init overlays
        MinecraftForge.EVENT_BUS.register(LittleTilesProfilerOverlay.class);
        LEVEL_HANDLERS.register(OVERLAY_RENDERER = new OverlayRenderer());
        
        ReloadableResourceManager reloadableResourceManager = (ReloadableResourceManager) mc.getResourceManager();
        reloadableResourceManager.registerReloadListener(new ResourceManagerReloadListener() {
            
            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                RenderingThread.CURRENT_RENDERING_INDEX++;
                LittleBlockClientRegistry.clearCache();
                ITEM_RENDER_CACHE.clearCache();
            }
        });
        
        CreativeCoreClient.registerClientConfig(LittleTiles.MODID);
        
        EntityRenderers.register(LittleTilesRegistry.SIZED_TNT_TYPE.get(), RenderSizedTNTPrimed::new);
        EntityRenderers.register(LittleTilesRegistry.ENTITY_LEVEL.get(), LittleEntityRenderer::new);
        EntityRenderers.register(LittleTilesRegistry.ENTITY_ANIMATION.get(), LittleEntityRenderer::new);
        EntityRenderers.register(LittleTilesRegistry.SIT_TYPE.get(), LittleSitRenderer::new);
        
        blockEntityRenderer = new BETilesRenderer();
        BlockEntityRenderers.register(LittleTilesRegistry.BE_TILES_TYPE_RENDERED.get(), x -> blockEntityRenderer);
        
        ResourceLocation filled = new ResourceLocation(LittleTiles.MODID, "filled");
        ClampedItemPropertyFunction function = (stack, level, entity, x) -> ((ItemColorIngredient) stack.getItem()).getColor(stack) / (float) ColorIngredient.BOTTLE_SIZE;
        ItemProperties.register(LittleTilesRegistry.BLACK_COLOR.get(), filled, function);
        ItemProperties.register(LittleTilesRegistry.CYAN_COLOR.get(), filled, function);
        ItemProperties.register(LittleTilesRegistry.MAGENTA_COLOR.get(), filled, function);
        ItemProperties.register(LittleTilesRegistry.YELLOW_COLOR.get(), filled, function);
    }
    
    public static void modelLoader(RegisterAdditional event) {
        event.register(new ModelResourceLocation(LittleTiles.MODID, "glove_background", "inventory"));
        event.register(new ModelResourceLocation(LittleTiles.MODID, "chisel_background", "inventory"));
        event.register(new ModelResourceLocation(LittleTiles.MODID, "blueprint_background", "inventory"));
    }
    
    public static void modelEvent(RegisterGeometryLoaders event) {
        CreativeCoreClient.registerBlockModel(new ResourceLocation(LittleTiles.MODID, "empty"), new CreativeBlockModel() {
            
            @Override
            public List<? extends RenderBox> getBoxes(BlockState state, ModelData data, RandomSource source) {
                return Collections.EMPTY_LIST;
            }
            
            @Override
            public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
                return modelData;
            }
        });
        
        CreativeCoreClient.registerItemModel(new ResourceLocation(LittleTiles.MODID, "tiles"), new LittleModelItemTilesBig());
        CreativeCoreClient.registerItemModel(new ResourceLocation(LittleTiles.MODID, "premade"), new LittleModelItemTilesBig() {
            @Override
            public List<? extends RenderBox> getBoxes(ItemStack stack, boolean translucent) {
                if (!stack.getOrCreateTag().contains(LittleGroup.STRUCTURE_KEY))
                    return Collections.EMPTY_LIST;
                
                LittlePremadeType premade = ItemPremadeStructure.get(stack);
                if (premade == null)
                    return Collections.EMPTY_LIST;
                LittleGroup previews = ((ItemPremadeStructure) stack.getItem()).getTiles(stack);
                if (previews == null)
                    return Collections.EMPTY_LIST;
                List<RenderBox> cubes = premade.getItemPreview(previews, translucent);
                if (cubes == null) {
                    cubes = previews.getRenderingBoxes(translucent);
                    LittleGroup.shrinkCubesToOneBlock(cubes);
                }
                
                return cubes;
            }
        });
        CreativeCoreClient.registerItemModel(new ResourceLocation(LittleTiles.MODID, "glove"),
            new LittleModelItemPreview(new ModelResourceLocation(LittleTiles.MODID, "glove_background", "inventory"), null) {
                
                @Override
                public boolean shouldRenderFake(ItemStack stack) {
                    return true;
                }
                
                @Override
                protected ItemStack getFakeStack(ItemStack current) {
                    GloveMode mode = ItemLittleGlove.getMode(current);
                    if (mode.hasPreviewElement(current))
                        return new ItemStack(mode.getPreviewElement(current).getState().getBlock());
                    
                    if (!mode.hasTiles(current))
                        return ItemStack.EMPTY;
                    
                    ItemStack stack = new ItemStack(LittleTilesRegistry.ITEM_TILES.get());
                    stack.setTag(LittleGroup.save(mode.getTiles(current)));
                    return stack;
                    
                }
            });
        CreativeCoreClient.registerItemModel(new ResourceLocation(LittleTiles.MODID, "chisel"),
            new LittleModelItemPreview(new ModelResourceLocation(LittleTiles.MODID, "chisel_background", "inventory"), stack -> ItemLittleChisel.getElement(stack)));
        CreativeCoreClient.registerItemModel(new ResourceLocation(LittleTiles.MODID, "blueprint"),
            new LittleModelItemBackground(new ModelResourceLocation(LittleTiles.MODID, "blueprint_background", "inventory"), x -> {
                ItemStack stack = new ItemStack(LittleTilesRegistry.ITEM_TILES.get());
                stack.setTag(x.getOrCreateTag().getCompound(ItemLittleBlueprint.CONTENT_KEY));
                return stack;
            }));
        
        CreativeCoreClient.registerItemModel(new ResourceLocation(LittleTiles.MODID, "blockingredient"),
            new CreativeItemBoxModel(new ModelResourceLocation("minecraft", "stone", "inventory")) {
                
                @Override
                public List<? extends RenderBox> getBoxes(ItemStack stack, boolean translucent) {
                    List<RenderBox> cubes = new ArrayList<>();
                    BlockIngredientEntry ingredient = ItemBlockIngredient.loadIngredient(stack);
                    if (ingredient == null)
                        return null;
                    
                    double volume = Math.min(1, ingredient.value);
                    LittleGrid context = LittleGrid.overallDefault();
                    long pixels = (long) (volume * context.count3d);
                    if (pixels < context.count * context.count)
                        cubes.add(new RenderBox(0.4F, 0.4F, 0.4F, 0.6F, 0.6F, 0.6F, ingredient.block.getState()));
                    else {
                        long remainingPixels = pixels;
                        long planes = pixels / context.count2d;
                        remainingPixels -= planes * context.count2d;
                        long rows = remainingPixels / context.count;
                        remainingPixels -= rows * context.count;
                        
                        float height = (float) (planes * context.pixelLength);
                        
                        if (planes > 0)
                            cubes.add(new RenderBox(0.0F, 0.0F, 0.0F, 1.0F, height, 1.0F, ingredient.block.getState()));
                        
                        float width = (float) (rows * context.pixelLength);
                        
                        if (rows > 0)
                            cubes.add(new RenderBox(0.0F, height, 0.0F, 1.0F, height + (float) context.pixelLength, width, ingredient.block.getState()));
                        
                        if (remainingPixels > 0)
                            cubes.add(new RenderBox(0.0F, height, width, 1.0F, height + (float) context.pixelLength, width + (float) context.pixelLength, ingredient.block
                                    .getState()));
                    }
                    return cubes;
                }
            });
    }
    
    public static void initItemColors(RegisterColorHandlersEvent.Item event) {
        CreativeCoreClient.registerItemColor(event.getItemColors(), LittleTilesRegistry.PREMADE.get());
        CreativeCoreClient.registerItemColor(event.getItemColors(), LittleTilesRegistry.ITEM_TILES.get());
        event.register((stack, tint) -> {
            if (stack.getItem() instanceof BlockItem block)
                return event.getBlockColors().getColor(block.getBlock().defaultBlockState(), (BlockAndTintGetter) null, (BlockPos) null, tint);
            return ColorUtils.WHITE;
        }, LittleTilesRegistry.WATER.get(), LittleTilesRegistry.FLOWING_WATER.get());
    }
    
    public static void initBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> level != null && pos != null ? BiomeColors.getAverageWaterColor(level, pos) : 4159204, LittleTilesRegistry.WATER.get(),
            LittleTilesRegistry.FLOWING_WATER.get());
    }
    
    public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("lt-debug").executes(x -> {
            if (LittleTilesProfilerOverlay.isActive())
                LittleTilesProfilerOverlay.stop();
            else
                LittleTilesProfilerOverlay.start();
            return Command.SINGLE_SUCCESS;
        }));
    }
    
    public static class LittleKeyMapping extends KeyMapping {
        
        private boolean ignoreModifier = false;
        
        public LittleKeyMapping(String description, IKeyConflictContext keyConflictContext, int keyCode, String category) {
            super(description, keyConflictContext, KeyModifier.NONE, InputConstants.Type.KEYSYM, keyCode, category);
        }
        
        public LittleKeyMapping(String description, IKeyConflictContext keyConflictContext, KeyModifier keyModifier, int keyCode, String category) {
            super(description, keyConflictContext, keyModifier, InputConstants.Type.KEYSYM, keyCode, category);
        }
        
        public LittleKeyMapping ignoreModifier() {
            ignoreModifier = true;
            return this;
        }
        
        @Override
        public boolean isActiveAndMatches(Key keyCode) {
            if (ignoreModifier)
                return keyCode != InputConstants.UNKNOWN && keyCode.equals(getKey());
            return super.isActiveAndMatches(keyCode);
        }
        
        @Override
        public boolean same(KeyMapping other) {
            if (other instanceof LittleKeyMapping && super.same(other))
                return true;
            return false;
        }
    }
    
}
