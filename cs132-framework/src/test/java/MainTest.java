import java.util.function.*;

import org.junit.Test;
import static org.junit.Assert.*;

class Pair {
    final int x;
    final Lazy<Pair> n;

    Pair(int x, Function<Pair, Pair> n) {
        this.x = x;
        this.n = new Lazy<>(() -> n.apply(this));
    }

    @Override
    public String toString() {
        final var k = n.get();
        if (k == this)
            return x + " -> <cycle>";
        else
            return x + " -> " + k.toString();
    }
}

public class MainTest {
    @Test
    public void testLazyList() {
        // System.out.println("=== r1 ===");

        // final var l1 = List.of(1).cons(2).cons(3).cons(4).cons(5).cons(6);
        // final var r1 = l1.map(x -> x * 2).get().find(x -> x < 9).get();

        // System.out.println("Result: " + r1);

        // System.out.println("=== r2 ===");

        // final var l2 = List.of(1).cons(2).cons(3);
        // final var r2 = l1.join(l2).map(x -> x * 2);

        // r2.fold(0, (u, x) -> {
        //     System.out.println(x);
        //     return 0;
        // });

        // System.out.println("=== r3 ===");

        // final var l3 = l2.flatMap(x -> List.of(1).cons(2).cons(3).map(y -> y * x));
        // final var r3 = l3.find(x -> x < 3);

        // System.out.println("Result: " + r3);
    }

    @Test
    public void testCycle() {
        final var a = new Pair(0, (p) -> p);
        System.out.println(a);
    }
}
