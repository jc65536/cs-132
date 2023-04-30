import org.junit.Test;
import static org.junit.Assert.*;

public class MainTest {
    @Test
    public void testLazyList() {
        System.out.println("=== r1 ===");

        final var l1 = List.of(1).cons(2).cons(3).cons(4).cons(5).cons(6);
        final var r1 = l1.map(x -> x * 2).get().find(x -> x < 9).get();

        System.out.println("Result: " + r1);

        System.out.println("=== r2 ===");

        final var l2 = List.of(1).cons(2).cons(3);
        final var r2 = l1.join(l2).bind(l -> l.map(x -> x * 2));

        r2.get().fold(0, (x, e) -> {
            System.out.println(e);
            return 0;
        });

        System.out.println("=== r3 ===");

        final var r3 = l2.flatMap(x -> List.of(1).cons(2).cons(3).map(y -> y * x)).get().find(x -> x < 6).get();

        System.out.println("Result: " + r3);
    }
}
