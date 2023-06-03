import java.util.*;
import java.util.function.*;

interface ResInt<T> {
    ResumableList<T> join(ResumableList<T> l);
}

public class ResumableList<T> extends Lazy<ResState<T>> implements ResInt<T> {
    ResumableList(List<T> l) {
        this(l.fold(nul(), ResumableList::cons));
    }

    ResumableList(Supplier<ResState<T>> s) {
        super(s);
    }

    static <T> ResumableList<T> nul() {
        return new ResumableList<>(() -> new Null<>());
    }

    ResumableList<T> cons(T val) {
        return new ResumableList<>(() -> new Pair<>(val, this));
    }

    @Override
    public ResumableList<T> join(ResumableList<T> l) {
        return new ResumableList<>(bind(s -> s.join(l)));
    }
}

abstract class ResState<T> implements ResInt<T> {
}

class Pair<T> extends ResState<T> {
    final T val;
    final ResumableList<T> next;

    Pair(T val, ResumableList<T> next) {
        this.val = val;
        this.next = next;
    }

    @Override
    public ResumableList<T> join(ResumableList<T> l) {
        return next.join(l).cons(val);
    }
}

class Pause<T> extends ResState<T> {
    final ResumableList<T> next;

    Pause(ResumableList<T> next) {
        this.next = next;
    }

    @Override
    public ResumableList<T> join(ResumableList<T> l) {
        final var lstate = l.get();
        if (lstate instanceof Pair) {
            return l.join(new ResumableList<>(() -> this));
        } else if (lstate instanceof Pause) {
            return new ResumableList<>(() -> new Pause<>(next.join(((Pause<T>) lstate).next)));
        } else {
            return new ResumableList<>(() -> this);
        }
    }
}

class Null<T> extends ResState<T> {
    @Override
    public ResumableList<T> join(ResumableList<T> l) {
        return l;
    }
}
