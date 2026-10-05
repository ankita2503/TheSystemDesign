# Consistent hashing: a standalone Java POC

This small console program answers: **Which server owns a key, and how many
assignments change when a server joins or leaves?** Servers are strings. There
are no services to start, dependencies to download, or actual data transfers.

## Run

Requires JDK 17 or later. From this folder:

```sh
sh run.sh
```

The script compiles the Java files, runs executable tests, then prints experiments
on 10,000 keys. It creates only an `out/` directory. No Maven or Gradle required.

## Read the code in this order

1. `ModuloRouter.java`: hash a key, then take the remainder modulo server count.
2. `ConsistentHashRouter.java`: read `locate` first, then `addServer` and `removeServer`.
3. `Experiment.java`: capture assignments before and after a membership change.
4. `ConsistentHashRouterTest.java`: exact examples plus membership properties.
5. `Hashing.java`: converts UTF-8 text into a deterministic position using SHA-256.

All source files are under `src/main/java/poc/`; tests are under `src/test/java/poc/`.

## How the ring works

Both server tokens and keys occupy the same ordered numeric space. We use
nonnegative 63-bit positions from SHA-256. A TreeMap keeps server tokens sorted.
The first token at or after a key's hash owns that key. If there is no later token,
lookup wraps to the first token. A key exactly on a token belongs to that token.

A small example, using hand-picked positions rather than real hash output:

```text
0 --- A:10 -------- B:40 -------- C:80 --- end
^                                         |
+---------------- wrap -------------------+

key at 25 -> B
key at 90 -> A
```

Adding D at 60 moves only keys in (40, 60] from C to D. Removing B transfers its
range to the next token. Existing token positions never change.

The production-size numeric range used here is fixed; it does not depend on the
number of servers. Unlike modulo routing, adding a server does not change a
divisor applied to every key.

## Virtual nodes

With 100 positions per server, names such as `server-A#0` through `server-A#99`
are independently hashed. These are metadata entries, not extra machines or
copies of data. Lookup returns the physical name `server-A`.

More positions generally improve balance. They do not guarantee exact balance,
nor do they solve a single heavily requested key. Equal numbers of keys also do
not imply equal byte sizes or request rates.

## Understanding the output

Each experiment starts with A, B, C. Addition appends D; removal removes B.
Both strategies use the same hash function and same keys. Movement compares
physical server names, not token positions. Zero-load servers remain in counts.

- Modulo routing from 3 to 4 servers should move roughly 75% of uniform keys.
- A well-balanced ring should move roughly 25% when a fourth equal server joins.
- A one-position ring may be very uneven, so its movement can differ substantially.
- Removing a server from the ring should move only that server's keys.
- `min` and `max` show the smallest and largest resulting per-server key counts.

Do not interpret fewer moved keys alone as better: a new server receiving almost
no keys causes little movement but also contributes little capacity.

## Explicit decisions and limits

- Single-threaded: concurrent mutation and lookup are not supported.
- Empty-ring lookup throws IllegalStateException.
- Null/blank keys and server IDs are rejected; text is not trimmed or normalized.
- Re-adding a server or removing an unknown server is a no-op.
- Token collisions reject the entire addition without changing existing ownership.
  This makes a rare event explicit instead of silently overwriting another token.
- Server IDs must stay stable. Routers must agree on membership and hash settings.
- Replication, failure detection, persistence, network calls, traffic balancing,
  and migrating existing data are outside this POC.
- SHA-256 is used for a portable, well-distributed teaching example; no performance
  claim is made. Lookup is O(log T), where T is total tokens; removal scans tokens.

## Experiments to try yourself

1. Run the program twice. Why are the outputs identical?
2. Try 5, 50, and 500 positions per server. Compare both balance and movement.
3. Change the server identifiers. Observe the effect on a one-position ring.
4. Add a fifth server and measure movement again.
5. Weight keys with different request counts. Is a balanced key count enough?

The tests coordinate no threads because this POC has no concurrency. They check
exact boundaries, wraparound, validation, collision atomicity, deterministic
placement independent of insertion order, addition/removal properties over
10,000 keys, and the last server leaving.
