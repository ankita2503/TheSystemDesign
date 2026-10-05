package poc;

import java.util.HashSet;
import java.util.List;

/** Baseline: changing the list size changes the divisor for every key. */
public final class ModuloRouter {
    private final List<String> servers;

    public ModuloRouter(List<String> servers) {
        if (servers == null || servers.isEmpty()) throw new IllegalArgumentException("Servers required");
        for (String server : servers) {
            if (server == null || server.isBlank()) throw new IllegalArgumentException("Invalid server");
        }
        if (new HashSet<>(servers).size() != servers.size()) throw new IllegalArgumentException("Duplicate server");
        this.servers = List.copyOf(servers);
    }

    public String locate(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Key required");
        return servers.get((int) (Hashing.position(key) % servers.size()));
    }
}
