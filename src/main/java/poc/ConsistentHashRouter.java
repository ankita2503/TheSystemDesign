package poc;

import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.ToLongFunction;

/** Educational, single-threaded router. Stores placement metadata, not user data. */
public final class ConsistentHashRouter {
    private final int virtualNodes;
    private final ToLongFunction<String> hash;
    private final NavigableMap<Long, String> ring = new TreeMap<>();
    private final Set<String> servers = new HashSet<>();

    public ConsistentHashRouter(int virtualNodes) {
        this(virtualNodes, Hashing::position);
    }

    // Package-private injection makes exact positions and collisions testable.
    ConsistentHashRouter(int virtualNodes, ToLongFunction<String> hash) {
        if (virtualNodes <= 0) throw new IllegalArgumentException("virtualNodes must be positive");
        this.virtualNodes = virtualNodes;
        this.hash = java.util.Objects.requireNonNull(hash);
    }

    public void addServer(String serverId) {
        requireText(serverId, "serverId");
        if (servers.contains(serverId)) return; // Repeated addition is harmless.

        // Validate every position before modifying the ring: failure is atomic.
        NavigableMap<Long, String> additions = new TreeMap<>();
        for (int i = 0; i < virtualNodes; i++) {
            long position = hash.applyAsLong(serverId + "#" + i);
            if (ring.containsKey(position) || additions.containsKey(position)) {
                // Rare with 63 bits, but never silently overwrite another owner.
                throw new IllegalStateException("Hash position collision while adding " + serverId);
            }
            additions.put(position, serverId);
        }
        ring.putAll(additions);
        servers.add(serverId);
    }

    public void removeServer(String serverId) {
        requireText(serverId, "serverId");
        if (!servers.remove(serverId)) return;
        ring.entrySet().removeIf(entry -> entry.getValue().equals(serverId));
    }

    public String locate(String key) {
        requireText(key, "key");
        if (ring.isEmpty()) throw new IllegalStateException("Cannot route on an empty ring");
        long position = hash.applyAsLong(key);
        // First token at or after the key, equivalent to moving clockwise.
        Map.Entry<Long, String> owner = ring.ceilingEntry(position);
        // Past the final token? Continue around the circle to the first token.
        if (owner == null) owner = ring.firstEntry();
        return owner.getValue();
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must be nonblank");
        }
    }
}
