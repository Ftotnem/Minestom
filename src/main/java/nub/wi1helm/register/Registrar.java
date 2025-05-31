package nub.wi1helm.register;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minestom.server.MinecraftServer; // Assuming this is still used for context, though not directly in the registration logic
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// Import gRPC specific classes
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import build.buf.gen.minekube.gate.v1.*; // Import all Gate gRPC definitions

public class Registrar {

    private static final Logger logger = LoggerFactory.getLogger(Registrar.class);
    private static final Gson gson = new Gson();

    private static final String REDIS_REGISTRY_HASH_PREFIX = "services:";
    private static final String SERVICE_TYPE_PROXY = "proxy";
    private static final String SERVICE_TYPE_MINESTOM = "minestom";

    // Discovery interval for Gate proxy endpoints in Redis
    private static final long PROXY_DISCOVERY_INTERVAL_SECONDS = 10;
    // Interval to check registration status with Gate proxies using listServers
    private static final long REGISTRATION_CHECK_INTERVAL_SECONDS = 5;
    // Delay before retrying registration if it fails or is not found
    private static final long REGISTRATION_RETRY_DELAY_SECONDS = 5;

    private final String minestomServiceId;
    private final String minestomPodIp;
    private final int minestomPort;
    private final String minestomServerLabel; // This will be the "name" for gRPC
    private final String minestomVersion;

    private final JedisCluster jedisCluster;
    // Stores all currently known active proxies.
    private final ConcurrentHashMap<String, ProxyInfo> knownProxies;
    // Stores gRPC stubs for each proxy this server is registered with.
    private final ConcurrentHashMap<String, GateProxyConnection> activeGateConnections;


    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    private static class ProxyInfo {
        String serviceId;
        String ip;
        int port; // This should be the HTTP/gRPC API port of the proxy

        public ProxyInfo() {}
        public ProxyInfo(String serviceId, String ip, int port) {
            this.serviceId = serviceId;
            this.ip = ip;
            this.port = port;
        }

        @Override
        public String toString() {
            return "ProxyInfo{" +
                    "serviceId='" + serviceId + '\'' +
                    ", ip='" + ip + '\'' +
                    ", port=" + port +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProxyInfo proxyInfo = (ProxyInfo) o;
            return port == proxyInfo.port &&
                    serviceId.equals(proxyInfo.serviceId) &&
                    ip.equals(proxyInfo.ip);
        }

        @Override
        public int hashCode() {
            int result = serviceId.hashCode();
            result = 31 * result + ip.hashCode();
            result = 31 * result + port;
            return result;
        }
    }

    // Encapsulates a gRPC connection to a Gate proxy
    private static class GateProxyConnection {
        ManagedChannel channel;
        GateServiceGrpc.GateServiceBlockingStub stub;
        ProxyInfo proxyInfo;
        // Keep track of the last successful contact (listServers or registerServer)
        volatile long lastSuccessfulContact;
        volatile boolean registered; // True if this server is believed to be registered with this proxy

        public GateProxyConnection(ProxyInfo proxyInfo) {
            this.proxyInfo = proxyInfo;
            this.lastSuccessfulContact = 0; // Not contacted yet
            this.registered = false;
            initChannelAndStub();
        }

        private void initChannelAndStub() {
            if (channel != null && !channel.isShutdown()) {
                channel.shutdownNow();
            }
            channel = ManagedChannelBuilder
                    .forAddress(proxyInfo.ip, proxyInfo.port)
                    .usePlaintext() // For development/unsecured connections - consider mTLS in production!
                    .build();
            stub = GateServiceGrpc.newBlockingStub(channel);
            logger.info("gRPC client initialized for Gate at {}:{}", proxyInfo.ip, proxyInfo.port);
        }

        public void shutdown() {
            if (channel != null) {
                logger.info("Shutting down gRPC channel for Gate at {}:{}", proxyInfo.ip, proxyInfo.port);
                channel.shutdown();
                try {
                    if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                        logger.warn("gRPC channel for {}:{} did not terminate in time. Forcing shutdown.",
                                proxyInfo.ip, proxyInfo.port);
                        channel.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    logger.warn("gRPC channel for {}:{} termination interrupted.", proxyInfo.ip, proxyInfo.port, e);
                    channel.shutdownNow();
                }
            }
        }
    }

    public static class ServiceInfo {
        public String service_id;
        public String service_type;
        public String ip;
        public int port;
        public long last_seen;
        public Map<String, String> metadata;

        public ServiceInfo(String serviceId, String serviceType, String ip, int port, long lastSeen, Map<String, String> metadata) {
            this.service_id = serviceId;
            this.service_type = serviceType;
            this.ip = ip;
            this.port = port;
            this.last_seen = lastSeen;
            this.metadata = metadata;
        }

        public ServiceInfo() {}
    }


    /**
     * Initializes a new MinestomProxyRegistrar with the given configuration.
     *
     * @param config The RegistrarConfig containing all necessary settings.
     * @return A fully configured and initialized Registrar instance.
     * @throws IllegalStateException if Redis connection fails.
     */
    public static Registrar createAndConfigure(RegistrarConfig config) {
        JedisCluster jedisCluster;
        try {
            jedisCluster = new JedisCluster(config.getRedisClusterNodes());
            // Simple test to ensure connection
            jedisCluster.hgetAll("test_connection");
            logger.info("Successfully connected to Redis cluster: {}", config.getRedisClusterNodes());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to connect to Redis cluster using provided nodes: " + config.getRedisClusterNodes(), e);
        }

        return new Registrar(config, jedisCluster);
    }

    private Registrar(RegistrarConfig config, JedisCluster jedisCluster) {
        this.minestomServiceId = SERVICE_TYPE_MINESTOM + "-" + UUID.randomUUID().toString();
        this.minestomPodIp = getPodIp();
        this.minestomPort = config.getMinestomPort();
        this.minestomServerLabel = config.getMinestomServerLabel();
        this.minestomVersion = config.getMinestomVersion();

        this.jedisCluster = jedisCluster;
        this.knownProxies = new ConcurrentHashMap<>();
        this.activeGateConnections = new ConcurrentHashMap<>();

        logger.info("MinestomProxyRegistrar initialized for this server {}:{} (Label: {}) with ID: {}",
                minestomPodIp, minestomPort, minestomServerLabel, minestomServiceId);
    }

    private String getPodIp() {
        String podIp = System.getenv("POD_IP");
        if (podIp != null && !podIp.isEmpty()) {
            logger.info("Retrieved POD_IP from environment variable: {}", podIp);
            return podIp;
        }

        try {
            InetAddress localHost = InetAddress.getLocalHost();
            if (!localHost.isLoopbackAddress()) {
                logger.warn("POD_IP environment variable not set. Using InetAddress.getLocalHost() which might not be reliable in Kubernetes: {}", localHost.getHostAddress());
                return localHost.getHostAddress();
            }
        } catch (UnknownHostException e) {
            logger.error("Failed to get local host IP: {}", e.getMessage());
        }

        logger.warn("Could not determine POD_IP. Falling back to loopback address '127.0.0.1'. Ensure POD_IP env var is set in Kubernetes.");
        return "127.0.0.1";
    }

    public void start() {
        if (running) {
            logger.warn("MinestomProxyRegistrar is already running.");
            return;
        }
        running = true;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Minestom-Proxy-Reg-Scheduler");
            t.setDaemon(true);
            return t;
        });

        // Schedule proxy discovery and initial registration
        scheduler.scheduleAtFixedRate(this::discoverProxiesAndManageConnections, 0, PROXY_DISCOVERY_INTERVAL_SECONDS, TimeUnit.SECONDS);
        // Schedule periodic registration status checks
        scheduler.scheduleAtFixedRate(this::checkAndMaintainRegistrations, 0, REGISTRATION_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        logger.info("MinestomProxyRegistrar started. Registration checks every {}s, proxy discovery and connection management every {}s.",
                REGISTRATION_CHECK_INTERVAL_SECONDS, PROXY_DISCOVERY_INTERVAL_SECONDS);
    }

    public void stop() {
        if (!running) {
            logger.warn("MinestomProxyRegistrar is not running.");
            return;
        }
        running = false;
        logger.info("Signaling MinestomProxyRegistrar to stop...");

        // Deregister from all active Gate proxies
        activeGateConnections.forEach((id, conn) -> unregisterMinestomServerWithGate(conn));

        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    logger.warn("MinestomProxyRegistrar scheduler did not terminate in time. Forcing shutdown.");
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                logger.warn("MinestomProxyRegistrar scheduler termination interrupted.", e);
                scheduler.shutdownNow();
            }
        }
        // Shutdown all gRPC channels
        activeGateConnections.forEach((id, conn) -> conn.shutdown());

        jedisCluster.close();
        logger.info("MinestomProxyRegistrar stopped and JedisCluster closed.");
    }

    /**
     * Attempts to register this Minestom server with a specific Gate proxy via gRPC.
     *
     * @param connection The GateProxyConnection representing the target proxy.
     */
    private void registerMinestomServerWithGate(GateProxyConnection connection) {
        if (connection == null || connection.stub == null) {
            logger.warn("Cannot register with null or uninitialized GateProxyConnection.");
            return;
        }

        logger.info("Attempting to register Minestom server '{}' with Gate at {}:{}",
                minestomServerLabel, connection.proxyInfo.ip, connection.proxyInfo.port);
        RegisterServerRequest request = RegisterServerRequest.newBuilder()
                .setName(minestomServerLabel)
                .setAddress(minestomPodIp + ":" + minestomPort)
                .build();
        try {
            connection.stub.registerServer(request);
            connection.lastSuccessfulContact = Instant.now().toEpochMilli();
            connection.registered = true;
            logger.info("Server '{}' successfully registered with Gate at {}:{}",
                    minestomServerLabel, connection.proxyInfo.ip, connection.proxyInfo.port);
        } catch (StatusRuntimeException e) {
            logger.error("Failed to register server '{}' with Gate at {}:{}: {}",
                    minestomServerLabel, connection.proxyInfo.ip, connection.proxyInfo.port, e.getStatus().getDescription());
            connection.registered = false; // Mark as not registered
            // Consider more specific handling based on gRPC status codes (e.g., UNAVAILABLE, PERMISSION_DENIED)
        } catch (Exception e) {
            logger.error("Unexpected error during registration of server '{}' with Gate at {}:{}: {}",
                    minestomServerLabel, connection.proxyInfo.ip, connection.proxyInfo.port, e.getMessage(), e);
            connection.registered = false; // Mark as not registered
        }
    }

    /**
     * Attempts to unregister this Minestom server from a specific Gate proxy via gRPC.
     * This is primarily called on shutdown or if a proxy is removed from Redis.
     *
     * @param connection The GateProxyConnection representing the target proxy.
     */
    private void unregisterMinestomServerWithGate(GateProxyConnection connection) {
        if (connection == null || connection.stub == null) {
            logger.warn("Cannot unregister with null or uninitialized GateProxyConnection.");
            return;
        }

        logger.info("Attempting to unregister Minestom server '{}' from Gate at {}:{}",
                minestomServerLabel, connection.proxyInfo.ip, connection.proxyInfo.port);
        UnregisterServerRequest request = UnregisterServerRequest.newBuilder()
                .setName(minestomServerLabel)
                .build();
        try {
            connection.stub.unregisterServer(request);
            connection.registered = false;
            logger.info("Server '{}' successfully unregistered from Gate at {}:{}",
                    minestomServerLabel, connection.proxyInfo.ip, connection.proxyInfo.port);
        } catch (StatusRuntimeException e) {
            // If the proxy is already down, this is expected.
            logger.warn("Failed to unregister server '{}' from Gate at {}:{}: {} (Proxy might be down/unreachable)",
                    minestomServerLabel, connection.proxyInfo.ip, connection.proxyInfo.port, e.getStatus().getDescription());
            connection.registered = false;
        } catch (Exception e) {
            logger.error("Unexpected error during unregistration of server '{}' from Gate at {}:{}: {}",
                    minestomServerLabel, connection.proxyInfo.ip, connection.proxyInfo.port, e.getMessage(), e);
            connection.registered = false;
        }
    }

    /**
     * Checks the registration status of this Minestom server with each active Gate proxy
     * using `listServers`. If not found, it attempts to re-register.
     */
    private void checkAndMaintainRegistrations() {
        if (!running) return;

        logger.debug("Checking registration status with {} active Gate proxy connections...", activeGateConnections.size());
        for (Map.Entry<String, GateProxyConnection> entry : activeGateConnections.entrySet()) {
            String proxyId = entry.getKey();
            GateProxyConnection connection = entry.getValue();

            try {
                ListServersResponse response = connection.stub.listServers(ListServersRequest.getDefaultInstance());
                boolean found = response.getServersList().stream()
                        .anyMatch(server -> server.getName().equals(minestomServerLabel) &&
                                server.getAddress().equals(minestomPodIp + ":" + minestomPort));

                if (found) {
                    connection.lastSuccessfulContact = Instant.now().toEpochMilli();
                    connection.registered = true;
                    logger.debug("Server '{}' confirmed as registered with Gate at {}:{}",
                            minestomServerLabel, connection.proxyInfo.ip, connection.proxyInfo.port);
                } else {
                    logger.warn("Server '{}' not found in list of servers from Gate at {}:{}. Attempting to re-register.",
                            minestomServerLabel, connection.proxyInfo.ip, connection.proxyInfo.port);
                    connection.registered = false;
                    scheduler.schedule(() -> registerMinestomServerWithGate(connection), REGISTRATION_RETRY_DELAY_SECONDS, TimeUnit.SECONDS);
                }
            } catch (StatusRuntimeException e) {
                logger.warn("Failed to list servers from Gate at {}:{} (Proxy ID: {}): {}. Assuming registration lost, attempting re-registration.",
                        connection.proxyInfo.ip, connection.proxyInfo.port, proxyId, e.getStatus().getDescription());
                connection.registered = false; // Mark as not registered
                scheduler.schedule(() -> registerMinestomServerWithGate(connection), REGISTRATION_RETRY_DELAY_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.error("Unexpected error during registration check with Gate at {}:{} (Proxy ID: {}): {}. Attempting re-registration.",
                        connection.proxyInfo.ip, connection.proxyInfo.port, proxyId, e.getMessage(), e);
                connection.registered = false;
                scheduler.schedule(() -> registerMinestomServerWithGate(connection), REGISTRATION_RETRY_DELAY_SECONDS, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Discovers Gate proxy instances from Redis, manages their gRPC connections,
     * and ensures the Minestom server is registered with all active proxies.
     * This method handles adding new proxies, removing stale/dead ones, and re-initializing connections
     * if proxy IP/port changes.
     */
    private void discoverProxiesAndManageConnections() {
        logger.debug("Discovering Gate proxy gRPC endpoints from Redis...");
        String hashKey = REDIS_REGISTRY_HASH_PREFIX + SERVICE_TYPE_PROXY;
        Map<String, String> proxyEntries;
        try {
            proxyEntries = jedisCluster.hgetAll(hashKey);
        } catch (Exception e) {
            logger.error("Failed to retrieve proxy entries from Redis: {}", e.getMessage(), e);
            return;
        }

        Set<String> currentlyActiveRedisProxyIds = new HashSet<>();

        for (Map.Entry<String, String> entry : proxyEntries.entrySet()) {
            String proxyId = entry.getKey();
            String proxyJson = entry.getValue();
            try {
                ServiceInfo serviceInfo = gson.fromJson(proxyJson, ServiceInfo.class);
                if (serviceInfo == null || serviceInfo.ip == null || serviceInfo.port == 0) {
                    logger.warn("Skipping malformed proxy entry in Redis: {} -> {}", proxyId, proxyJson);
                    continue;
                }

                // Assuming the 'port' in ServiceInfo from Redis is the Gate's API port (e.g., 8080)
                int proxyApiPort = serviceInfo.metadata != null ?
                        Integer.parseInt(serviceInfo.metadata.getOrDefault("http_port", String.valueOf(8080))) : 8080;

                long now = Instant.now().toEpochMilli();
                // A proxy is considered stale if its last_seen is older than 3 registration check intervals
                if (now - serviceInfo.last_seen > (REGISTRATION_CHECK_INTERVAL_SECONDS * 1000 * 3)) {
                    logger.warn("Proxy {} (ID: {}) is stale, last seen {}ms ago. Skipping.",
                            serviceInfo.ip + ":" + proxyApiPort, proxyId, (now - serviceInfo.last_seen));
                    continue;
                }

                ProxyInfo newProxyInfo = new ProxyInfo(serviceInfo.service_id, serviceInfo.ip, proxyApiPort);
                currentlyActiveRedisProxyIds.add(proxyId);

                // Check if this proxy is new or has changed its IP/port
                ProxyInfo existingKnownProxy = knownProxies.get(proxyId);
                if (existingKnownProxy == null || !existingKnownProxy.equals(newProxyInfo)) {
                    knownProxies.put(proxyId, newProxyInfo);
                    logger.info("Discovered new/updated active Gate proxy gRPC endpoint: {}", newProxyInfo);

                    // If a new or changed proxy is found, create/re-initialize its connection
                    GateProxyConnection existingConnection = activeGateConnections.get(proxyId);
                    if (existingConnection != null) {
                        logger.info("Shutting down old connection for proxy {} due to IP/port change.", proxyId);
                        // Attempt to unregister from old endpoint before shutting down, though it might fail if proxy is truly gone
                        unregisterMinestomServerWithGate(existingConnection);
                        existingConnection.shutdown();
                    }
                    GateProxyConnection newConnection = new GateProxyConnection(newProxyInfo);
                    activeGateConnections.put(proxyId, newConnection);
                    // Attempt to register immediately with the new proxy
                    registerMinestomServerWithGate(newConnection);
                } else {
                    // Proxy exists and hasn't changed, ensure it's in activeGateConnections.
                    // Registration will be handled by checkAndMaintainRegistrations if needed.
                    activeGateConnections.computeIfAbsent(proxyId, k -> {
                        logger.info("Re-adding known proxy {} to active connections.", newProxyInfo);
                        GateProxyConnection conn = new GateProxyConnection(newProxyInfo);
                        // Initial registration attempt for a previously unknown but now active proxy
                        registerMinestomServerWithGate(conn);
                        return conn;
                    });
                }

            } catch (JsonSyntaxException e) {
                logger.warn("Failed to parse JSON for proxy {}: {}. Skipping.", proxyId, e.getMessage());
            } catch (NumberFormatException e) {
                logger.warn("Invalid 'http_port' metadata for proxy {}: {}. Using default 8080. Error: {}", proxyId, proxyJson, e.getMessage());
            }
        }

        // Remove proxies that are no longer present in Redis
        Set<String> removedProxyIds = knownProxies.keySet().stream()
                .filter(id -> !currentlyActiveRedisProxyIds.contains(id))
                .collect(Collectors.toSet());

        for (String removedId : removedProxyIds) {
            ProxyInfo removedProxyInfo = knownProxies.remove(removedId);
            GateProxyConnection connection = activeGateConnections.remove(removedId);
            if (connection != null) {
                logger.info("Gate proxy {} (ID: {}) no longer active in Redis. Unregistering and shutting down connection.",
                        removedProxyInfo.ip + ":" + removedProxyInfo.port, removedId);
                unregisterMinestomServerWithGate(connection);
                connection.shutdown();
            }
        }

        logger.debug("Finished Gate proxy discovery and connection management. Currently {} active known proxies.", knownProxies.size());

        if (knownProxies.isEmpty()) {
            logger.warn("No active Gate proxies discovered from Redis. Minestom server registration/updates might fail.");
        }
    }


    public String getMinestomPodIp() {
        return minestomPodIp;
    }

    public int getMinestomPort() {
        return minestomPort;
    }

    public String getMinestomServerLabel() {
        return minestomServerLabel;
    }
}
