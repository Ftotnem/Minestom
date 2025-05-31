package nub.wi1helm.register;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration class for the Registrar, loading settings from environment variables.
 * This class encapsulates all environment-specific details for the registration process.
 */
public class RegistrarConfig {

    private static final Logger logger = LoggerFactory.getLogger(RegistrarConfig.class);

    private final int minestomPort;
    private final String minestomServerLabel;
    private final String minestomVersion;
    private final Set<HostAndPort> redisClusterNodes;

    private RegistrarConfig(int minestomPort, String minestomServerLabel, String minestomVersion, Set<HostAndPort> redisClusterNodes) {
        this.minestomPort = minestomPort;
        this.minestomServerLabel = minestomServerLabel;
        this.minestomVersion = minestomVersion;
        this.redisClusterNodes = redisClusterNodes;
    }

    /**
     * Loads the configuration for the Registrar from environment variables.
     * This is the primary way to get an instance of RegistrarConfig for production.
     *
     * Required Environment Variables:
     * - MINECRAFT_SERVER_PORT: The port this Minestom server listens on (e.g., 25565).
     * - MINECRAFT_SERVER_LABEL: A unique label for this Minestom server (e.g., "lobby-1").
     * - MINESTOM_VERSION: The Minestom version or build number (for metadata).
     * - REDIS_CLUSTER_ADDR: Comma-separated list of Redis cluster nodes (e.g., "localhost:6379,192.168.1.10:6380").
     *
     * @return A new RegistrarConfig instance.
     * @throws IllegalStateException if any required environment variable is missing or malformed.
     */
    public static RegistrarConfig loadFromEnv() {
        logger.info("Loading Registrar configuration from environment variables...");

        int parsedMinestomPort = 0;
        String portStr = System.getenv("MINECRAFT_SERVER_PORT");
        if (portStr == null || portStr.isEmpty()) {
            throw new IllegalStateException("Environment variable MINECRAFT_SERVER_PORT is required.");
        }
        try {
            parsedMinestomPort = Integer.parseInt(portStr);
            if (parsedMinestomPort <= 0 || parsedMinestomPort > 65535) {
                throw new IllegalStateException("MINECRAFT_SERVER_PORT '" + portStr + "' is out of valid range (1-65535).");
            }
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid number format for MINECRAFT_SERVER_PORT: " + portStr, e);
        }

        String minestomServerLabel = System.getenv("MINECRAFT_SERVER_LABEL");
        if (minestomServerLabel == null || minestomServerLabel.isEmpty()) {
            throw new IllegalStateException("Environment variable MINECRAFT_SERVER_LABEL is required.");
        }

        String minestomVersion = System.getenv().getOrDefault("MINESTOM_VERSION", "Unknown");
        if ("Unknown".equals(minestomVersion)) {
            logger.warn("Environment variable MINESTOM_VERSION not set. Using default: 'Unknown'.");
        }

        String redisAddr = System.getenv("REDIS_CLUSTER_ADDR");
        if (redisAddr == null || redisAddr.isEmpty()) {
            throw new IllegalStateException("Environment variable REDIS_CLUSTER_ADDR is required.");
        }
        Set<HostAndPort> redisClusterNodes = parseRedisAddresses(redisAddr);
        if (redisClusterNodes.isEmpty()) {
            throw new IllegalStateException("No valid Redis cluster nodes could be parsed from REDIS_CLUSTER_ADDR: " + redisAddr);
        }

        logger.info("Registrar configuration loaded:");
        logger.info("  Minestom Port: {}", parsedMinestomPort);
        logger.info("  Minestom Label: {}", minestomServerLabel);
        logger.info("  Minestom Version: {}", minestomVersion);
        logger.info("  Redis Cluster Nodes: {}", redisClusterNodes);

        return new RegistrarConfig(parsedMinestomPort, minestomServerLabel, minestomVersion, redisClusterNodes);
    }

    /**
     * Creates a default development configuration for the Registrar.
     * This method is useful for local testing without requiring environment variables.
     *
     * @return A new RegistrarConfig instance with predefined development values.
     */
    public static RegistrarConfig loadDevConfig() {
        logger.info("Loading Registrar development configuration...");

        int devMinestomPort = 25567; // A common port for local testing
        String devMinestomServerLabel = "dev-lobby";
        String devMinestomVersion = "DEV_SNAPSHOT";

        Set<HostAndPort> devRedisNodes = new HashSet<>();
        devRedisNodes.add(new HostAndPort("localhost", 7000));
        devRedisNodes.add(new HostAndPort("localhost", 7001));
        devRedisNodes.add(new HostAndPort("localhost", 7002));
        devRedisNodes.add(new HostAndPort("localhost", 7003));
        devRedisNodes.add(new HostAndPort("localhost", 7004));
        devRedisNodes.add(new HostAndPort("localhost", 7005));

        logger.info("Registrar development configuration loaded:");
        logger.info("  Minestom Port: {}", devMinestomPort);
        logger.info("  Minestom Label: {}", devMinestomServerLabel);
        logger.info("  Minestom Version: {}", devMinestomVersion);
        logger.info("  Redis Cluster Nodes: {}", devRedisNodes);

        return new RegistrarConfig(devMinestomPort, devMinestomServerLabel, devMinestomVersion, devRedisNodes);
    }


    public int getMinestomPort() {
        return minestomPort;
    }

    public String getMinestomServerLabel() {
        return minestomServerLabel;
    }

    public String getMinestomVersion() {
        return minestomVersion;
    }

    public Set<HostAndPort> getRedisClusterNodes() {
        return redisClusterNodes;
    }

    private static Set<HostAndPort> parseRedisAddresses(String addresses) {
        Set<HostAndPort> nodes = new HashSet<>();
        for (String address : addresses.split(",")) {
            String trimmedAddress = address.trim();
            if (trimmedAddress.isEmpty()) {
                continue;
            }

            String[] parts = trimmedAddress.split(":");
            if (parts.length == 2) {
                try {
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);
                    if (port <= 0 || port > 65565) { // Fixed upper range for Minecraft ports
                        logger.error("Invalid port number '{}' in Redis address '{}'. Port must be between 1 and 65565.", port, trimmedAddress);
                        continue;
                    }
                    nodes.add(new HostAndPort(host, port));
                } catch (NumberFormatException e) {
                    logger.error("Invalid port number in Redis address '{}'. Expected format 'host:port'.", trimmedAddress);
                }
            } else {
                logger.error("Invalid Redis address format: '{}'. Expected 'host:port'.", trimmedAddress);
            }
        }
        return nodes;
    }
}