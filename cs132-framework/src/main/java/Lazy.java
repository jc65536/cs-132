import java.util.function.*;

public class Lazy<T> implements Supplier<T> {
    private T v;
    private Supplier<T> f;

    Lazy(Supplier<T> f) {
        this.f = f;
    }

    Lazy(Function<Lazy<T>, T> f) {
        this.f = () -> f.apply(this);
    }

    @Override
    public T get() {
        if (f != null) {
            v = f.get();
            f = null;
        }

        return v;
    }
}
