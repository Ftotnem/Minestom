package nub.wi1helm.server;

import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.color.Color;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.biome.Biome;
import net.minestom.server.world.biome.BiomeEffects;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ServerInstance extends net.minestom.server.instance.InstanceContainer {

    private int CHUNK_RADIUS_X = 4;
    private int CHUNK_RADIUS_Z = 2;

    public ServerInstance() {
        // CORRECTED LINE: Use the path relative to the /app WORKDIR in the Docker container
        // The resources are copied to /app/resources, so the world is at /app/resources/world
        super(UUID.randomUUID(), DimensionType.OVERWORLD, new AnvilLoader("resources/world"));
        this.enableAutoChunkLoad(false);
        // Store the futures so we can use CompletableFuture#allOf
        Set<CompletableFuture<Chunk>> futures = new HashSet<>();

        Biome biome = Biome.builder().effects(BiomeEffects.builder().skyColor(new Color(110, 177, 255)).waterColor(new Color(0,0,0)).waterFogColor(new Color(0,0,0)).fogColor(new Color(45,35,45)).build()).build();
        MinecraftServer.getBiomeRegistry().register(Key.key("main"),biome);

        setTime(1000);

        for (int x = -CHUNK_RADIUS_X; x <= CHUNK_RADIUS_X; x++) {
            for (int z = -CHUNK_RADIUS_Z; z <= CHUNK_RADIUS_Z; z++) {
                CompletableFuture<Chunk> future = this.loadChunk(x, z);

                // After chunk is loaded, set the biome for every block column inside the chunk
                future.thenAccept(chunk -> {
                    if (chunk != null) {
                        for (int bx = 0; bx < 16; bx++) {
                            for (int bz = 0; bz < 16; bz++) {
                                for (int by = -64; by < 256; by++) {
                                    chunk.setBiome(new BlockVec(bx, by, bz), DynamicRegistry.Key.of(Key.key("main")));
                                }
                            }
                        }
                    }
                });

                futures.add(future);
            }
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        MinecraftServer.getInstanceManager().registerInstance(this);
    }
}
