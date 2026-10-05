package poc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.IntStream;

public final class Experiment {
    private static final List<String> INITIAL = List.of("server-A", "server-B", "server-C");

    public static void main(String[] args) {
        List<String> keys = IntStream.range(0, 10_000).mapToObj(i -> "user:" + i).toList();
        System.out.println("10,000 identical keys; each experiment starts with A, B, C.");
        System.out.println("Movement compares physical server names. Counts are measured, not estimates.\n");
        for (boolean adding : new boolean[]{true, false}) {
            System.out.println(adding ? "ADD server-D" : "REMOVE server-B");
            System.out.printf("%-24s %10s  %s%n", "Strategy", "Moved", "Resulting keys per server");
            List<String> changed = new ArrayList<>(INITIAL);
            if (adding) changed.add("server-D"); else changed.remove("server-B");
            report("Modulo", assignments(keys, new ModuloRouter(INITIAL)::locate),
                    assignments(keys, new ModuloRouter(changed)::locate), changed);
            for (int positions : new int[]{1, 10, 100}) {
                ConsistentHashRouter router = new ConsistentHashRouter(positions);
                INITIAL.forEach(router::addServer);
                List<String> before = assignments(keys, router::locate);
                if (adding) router.addServer("server-D"); else router.removeServer("server-B");
                report("Ring, " + positions + " positions", before, assignments(keys, router::locate), changed);
            }
            System.out.println();
        }
        System.out.println("More positions generally improve balance, but this is not a guarantee.");
        System.out.println("No data is transferred: this program computes ownership only.");
    }

    private static List<String> assignments(List<String> keys, Function<String, String> router) {
        return keys.stream().map(router).toList();
    }

    private static void report(String label, List<String> before, List<String> after, List<String> servers) {
        int moved = 0;
        Map<String, Integer> counts = new TreeMap<>();
        servers.forEach(server -> counts.put(server, 0));
        for (int i = 0; i < before.size(); i++) {
            if (!before.get(i).equals(after.get(i))) moved++;
            counts.merge(after.get(i), 1, Integer::sum);
        }
        int min = counts.values().stream().mapToInt(Integer::intValue).min().orElseThrow();
        int max = counts.values().stream().mapToInt(Integer::intValue).max().orElseThrow();
        System.out.printf(java.util.Locale.ROOT, "%-24s %9.2f%%  %s (min=%d, max=%d)%n",
                label, 100.0 * moved / before.size(), counts, min, max);
    }
}
