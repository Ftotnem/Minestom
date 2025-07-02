package nub.wi1helm.server;

import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.color.Color;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.biome.Biome;
import net.minestom.server.world.biome.BiomeEffects;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ServerInstance extends InstanceContainer {

    private int CHUNK_RADIUS_X = 4;
    private int CHUNK_RADIUS_Z = 2;

    public ServerInstance() {
        // CORRECTED LINE: Use the path relative to the /app WORKDIR in the Docker container
        // The resources are copied to /app/resources, so the world is at /app/resources/world
        super(UUID.randomUUID(), DimensionType.OVERWORLD, new AnvilLoader("resources/world"));

    }
}
