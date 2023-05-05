import org.junit.Test;
import static org.junit.Assert.*;

public class MainTest {
    @Test
    public void testLazyList() {
        System.out.println("=== r1 ===");

        final var l1 = List.<Integer>nul().cons(1).cons(2).cons(3).cons(4).cons(5).cons(6);
        final var r1 = l1.map(x -> x * 2).get().find(x -> x < 9).get();

        System.out.println("Result: " + r1);

        System.out.println("=== r2 ===");

        // final var l2 = List.<Integer>nul().cons(1).cons(2).cons(3);
        // final var r2 = l1.join(l2).map(x -> x * 2);

        // r2.fold(0, (u, x) -> {
        //     System.out.println(x);
        //     return 0;
        // });

        // System.out.println("=== r3 ===");

        // final var r3 = l1.failFold(0, (u, x) -> Optional.of(u + x).filter(y -> y < 22));

        // System.out.println("Result: " + r3);

        // System.out.println("=== r3 ===");

        // final var l3 = l2.flatMap(x -> List.of(1).cons(2).cons(3).map(y -> y * x));
        // final var r3 = l3.find(x -> x < 3);

        // System.out.println("Result: " + r3);
    }
}
