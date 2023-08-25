package team.creative.littletiles.mixin.rubidium;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.lwjgl.opengl.GL15C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.mojang.blaze3d.platform.MemoryTracker;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferBuilder.SortState;
import com.mojang.blaze3d.vertex.VertexBuffer;

import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.client.gl.arena.PendingUpload;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferTarget;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshAttribute;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import team.creative.creativecore.common.util.type.list.Tuple;
import team.creative.creativecore.common.util.type.map.ChunkLayerMap;
import team.creative.littletiles.client.mod.rubidium.RubidiumInteractor;
import team.creative.littletiles.client.mod.rubidium.buffer.RubidiumChunkBufferDownloader;
import team.creative.littletiles.client.mod.rubidium.buffer.RubidiumChunkBufferUploader;
import team.creative.littletiles.client.render.cache.LayeredBufferCache;
import team.creative.littletiles.client.render.cache.buffer.BufferCache;
import team.creative.littletiles.client.render.cache.buffer.BufferCollection;
import team.creative.littletiles.client.render.cache.pipeline.LittleRenderPipelineType;
import team.creative.littletiles.client.render.mc.RenderChunkExtender;
import team.creative.littletiles.client.render.mc.VertexBufferExtender;

@Mixin(RenderSection.class)
public abstract class RenderSectionMixin implements RenderChunkExtender {
    
    @Shadow(remap = false)
    private int sectionIndex;
    
    @Shadow(remap = false)
    private int chunkX;
    
    @Shadow(remap = false)
    private int chunkY;
    
    @Shadow(remap = false)
    private int chunkZ;
    
    @Shadow(remap = false)
    private TextureAtlasSprite[] animatedSprites;
    
    @Unique
    private BlockPos origin;
    
    @Unique
    private volatile int queued;
    
    @Unique
    public ChunkLayerMap<BufferCollection> lastUploaded;
    
    @Override
    public int getQueued() {
        return queued;
    }
    
    @Override
    public void setQueued(int queued) {
        this.queued = queued;
    }
    
    @Override
    public ChunkLayerMap<BufferCollection> getLastUploaded() {
        return lastUploaded;
    }
    
    @Override
    public void setLastUploaded(ChunkLayerMap<BufferCollection> uploaded) {
        this.lastUploaded = uploaded;
    }
    
    @Override
    public void begin(BufferBuilder builder) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public VertexBuffer getVertexBuffer(RenderType layer) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void markReadyForUpdate(boolean playerChanged) {
        ((SodiumWorldRendererAccessor) SodiumWorldRenderer.instance()).getRenderSectionManager().scheduleRebuild(chunkX, chunkY, chunkZ, playerChanged);
    }
    
    @Override
    public void setQuadSorting(BufferBuilder builder, double x, double y, double z) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean isEmpty(RenderType layer) {
        return getUploadedBuffer(getStorage(getRenderRegion(), layer)) == null;
    }
    
    @Override
    public SortState getTransparencyState() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setHasBlock(RenderType layer) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public BlockPos standardOffset() {
        if (origin == null)
            origin = new BlockPos(chunkX * 16, chunkY * 16, chunkZ * 16);
        return origin;
    }
    
    @Override
    public LittleRenderPipelineType getPipeline() {
        return RubidiumInteractor.PIPELINE;
    }
    
    public GlBufferSegment getUploadedBuffer(SectionRenderDataStorage storage) {
        SectionRenderDataStorageAccessor s = (SectionRenderDataStorageAccessor) storage;
        if (s == null)
            return null;
        return s.getAllocations()[sectionIndex];
    }
    
    public SectionRenderDataStorage getStorage(RenderRegion region, RenderType layer) {
        return region.getStorage(DefaultMaterials.forRenderLayer(layer).pass);
    }
    
    public RenderRegion getRenderRegion() {
        return ((RenderSectionManagerAccessor) ((SodiumWorldRendererAccessor) SodiumWorldRenderer.instance()).getRenderSectionManager()).getRegions().createForChunk(chunkX, chunkY,
            chunkZ);
    }
    
    @Override
    public int sectionIndex() {
        return sectionIndex;
    }
    
    @Override
    public ByteBuffer downloadUploadedData(VertexBufferExtender buffer, long offset, int size) {
        RenderDevice.INSTANCE.createCommandList().bindBuffer(GlBufferTarget.ARRAY_BUFFER, (GlBuffer) buffer);
        try {
            ByteBuffer result = MemoryTracker.create(size);
            GL15C.glGetBufferSubData(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), offset, result);
            return result;
        } catch (IllegalArgumentException | IllegalStateException e) {
            if (!(e instanceof IllegalStateException))
                e.printStackTrace();
            return null;
        } finally {}
    }
    
    public ByteBuffer downloadSegment(GlBufferSegment segment) {
        GlBuffer buffer = ((GlBufferSegmentAccessor) segment).getArena().getBufferObject();
        return downloadUploadedData((VertexBufferExtender) buffer, segment.getOffset(), segment.getLength());
    }
    
    @Override
    public void backToRAM() {
        RenderRegion region = getRenderRegion();
        ChunkLayerMap<BufferCollection> caches = getLastUploaded();
        if (caches == null)
            return;
        
        Runnable run = () -> {
            RubidiumChunkBufferDownloader downloader = new RubidiumChunkBufferDownloader();
            RenderSectionManager manager = ((SodiumWorldRendererAccessor) SodiumWorldRenderer.instance()).getRenderSectionManager();
            ChunkBuilderAccessor chunkBuilder = (ChunkBuilderAccessor) manager.getBuilder();
            GlVertexFormat<ChunkMeshAttribute> format = ((ChunkBuildBuffersAccessor) chunkBuilder.getLocalContext().buffers).getVertexType().getVertexFormat();
            for (Tuple<RenderType, BufferCollection> tuple : caches.tuples()) {
                SectionRenderDataStorage storage = region.getStorage(DefaultMaterials.forRenderLayer(tuple.key).pass);
                if (storage == null)
                    continue;
                
                GlBufferSegment segment = getUploadedBuffer(storage);
                if (segment == null)
                    continue;
                ByteBuffer vertexData = downloadSegment(segment);
                
                if (vertexData == null) {
                    tuple.value.discard();
                    continue;
                }
                
                downloader.set(storage.getDataPointer(sectionIndex), format, downloadSegment(getUploadedBuffer(storage)));
                tuple.value.download(downloader);
                downloader.clear();
            }
            setLastUploaded(null);
        };
        try {
            CompletableFuture.runAsync(run, Minecraft.getInstance());
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
    
    @Override
    public boolean appendRenderData(Iterable<? extends LayeredBufferCache> blocks) {
        RenderSectionManager manager = ((SodiumWorldRendererAccessor) SodiumWorldRenderer.instance()).getRenderSectionManager();
        RenderRegion region = getRenderRegion();
        ChunkBuilderAccessor chunkBuilder = (ChunkBuilderAccessor) manager.getBuilder();
        GlVertexFormat<ChunkMeshAttribute> format = ((ChunkBuildBuffersAccessor) chunkBuilder.getLocalContext().buffers).getVertexType().getVertexFormat();
        RubidiumChunkBufferUploader uploader = new RubidiumChunkBufferUploader();
        
        for (RenderType layer : RenderType.chunkBufferLayers()) {
            
            int size = 0;
            for (LayeredBufferCache data : blocks)
                size += data.length(layer);
            
            if (size == 0)
                continue;
            
            SectionRenderDataStorage storage = region.createStorage(DefaultMaterials.forRenderLayer(layer).pass);
            
            GlBufferSegment segment = getUploadedBuffer(storage);
            ByteBuffer vanillaBuffer = null;
            if (segment != null) {
                vanillaBuffer = downloadSegment(getUploadedBuffer(storage));
                storage.removeMeshes(sectionIndex);
            }
            
            int[] extraLengthFacing = new int[ModelQuadFacing.COUNT];
            for (LayeredBufferCache layeredCache : blocks)
                for (int i = 0; i < extraLengthFacing.length; i++)
                    extraLengthFacing[i] += layeredCache.length(layer, i);
                
            uploader.set(storage.getDataPointer(sectionIndex), format, vanillaBuffer, size, extraLengthFacing, animatedSprites);
            
            for (LayeredBufferCache layeredCache : blocks) {
                BufferCache cache = layeredCache.get(layer);
                if (cache != null && cache.isAvailable())
                    cache.upload(uploader);
            }
            
            // Maybe sort uploaded buffer????
            //if (layer == RenderType.translucent())
            
            CommandList commandList = RenderDevice.INSTANCE.createCommandList();
            RenderRegion.DeviceResources resources = region.createResources(commandList);
            GlBufferArena arena = resources.getGeometryArena();
            PendingUpload upload = new PendingUpload(uploader.buffer());
            boolean bufferChanged = arena.upload(commandList, Stream.of(upload));
            if (bufferChanged)
                region.refresh(commandList);
            
            storage.setMeshes(sectionIndex, upload.getResult(), uploader.ranges());
            
            animatedSprites = uploader.sprites();
            
            uploader.clear();
            
        }
        return true;
    }
    
}