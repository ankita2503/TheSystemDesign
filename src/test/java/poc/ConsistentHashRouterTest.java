package poc;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/** Dependency-free executable tests; checks do not depend on enabling JVM assertions. */
public final class ConsistentHashRouterTest {
    public static void main(String[] args) {
        Map<String, Long> positions = Map.of("A#0", 10L, "B#0", 40L, "C#0", 80L,
                "five", 5L, "ten", 10L, "twenty-five", 25L, "sixty", 60L, "ninety", 90L);
        ConsistentHashRouter fixed = new ConsistentHashRouter(1, positions::get);
        expect(IllegalStateException.class, () -> fixed.locate("five"));
        List.of("A", "B", "C").forEach(fixed::addServer);
        equal("A", fixed.locate("five"));
        equal("A", fixed.locate("ten"));
        equal("B", fixed.locate("twenty-five"));
        equal("C", fixed.locate("sixty"));
        equal("A", fixed.locate("ninety"));
        fixed.addServer("A");
        fixed.removeServer("missing");
        equal("B", fixed.locate("twenty-five"));
        expect(IllegalArgumentException.class, () -> fixed.locate(" "));
        expect(IllegalArgumentException.class, () -> fixed.addServer(null));
        expect(IllegalArgumentException.class, () -> fixed.removeServer(""));
        expect(IllegalArgumentException.class, () -> new ConsistentHashRouter(0));

        ConsistentHashRouter collision = new ConsistentHashRouter(1, value -> 7L);
        collision.addServer("A");
        expect(IllegalStateException.class, () -> collision.addServer("B"));
        equal("A", collision.locate("key"));
        collision.removeServer("B");
        equal("A", collision.locate("key"));

        // A collision in a later virtual node must not leave an earlier node installed.
        ConsistentHashRouter partial = new ConsistentHashRouter(2, value -> switch (value) {
            case "A#0" -> 10; case "A#1" -> 40;
            case "B#0" -> 20; case "B#1" -> 40; default -> 15;
        });
        partial.addServer("A");
        expect(IllegalStateException.class, () -> partial.addServer("B"));
        equal("A", partial.locate("key"));

        List<String> keys = IntStream.range(0, 10_000).mapToObj(i -> "user:" + i).toList();
        for (int vnodes : new int[]{1, 10, 100}) {
            ConsistentHashRouter ring = new ConsistentHashRouter(vnodes);
            ConsistentHashRouter reverse = new ConsistentHashRouter(vnodes);
            List.of("A", "B", "C").forEach(ring::addServer);
            List.of("C", "B", "A").forEach(reverse::addServer);
            List<String> before = keys.stream().map(ring::locate).toList();
            equal(before, keys.stream().map(reverse::locate).toList());
            ring.addServer("D");
            for (int i = 0; i < keys.size(); i++) {
                String after = ring.locate(keys.get(i));
                if (!after.equals(before.get(i))) equal("D", after);
            }
            ring.removeServer("D");
            equal(before, keys.stream().map(ring::locate).toList());
            ring.removeServer("B");
            for (int i = 0; i < keys.size(); i++) {
                String after = ring.locate(keys.get(i));
                check(!after.equals("B"), "Removed server still owns a key");
                if (!before.get(i).equals("B")) equal(before.get(i), after);
            }
            ring.removeServer("A");
            keys.forEach(key -> equal("C", ring.locate(key)));
            ring.removeServer("C");
            expect(IllegalStateException.class, () -> ring.locate("key"));
        }
        System.out.println("PASS: boundaries, wraparound, validation, collisions, determinism, and membership properties.");
    }

    private static void equal(Object expected, Object actual) {
        check(expected.equals(actual), "Expected " + expected + ", got " + actual);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("Expected " + type.getSimpleName(), error);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
