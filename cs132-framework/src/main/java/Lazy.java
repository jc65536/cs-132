import java.util.Optional;
import java.util.function.*;

public class Lazy<T> implements Supplier<T> {
    T v;
    Supplier<T> s;

    Lazy(Supplier<T> s) {
        this.s = s;
    }

    Lazy(Function<Lazy<T>, T> f) {
        this.s = () -> f.apply(this);
    }

    @Override
    public T get() {
        if (s != null) {
            v = s.get();
            s = null;
        }

        return v;
    }

    <U> Lazy<U> bind(Function<T, Lazy<U>> f) {
        return new Lazy<>(() -> f.apply(get()).get());
    }
}
