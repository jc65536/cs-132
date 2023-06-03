import java.util.*;
import java.util.function.*;

interface ResInt<T> {
    ResumableList<T> join(ResumableList<T> l);

    ResumableList<T> unique(List<T> hist, BiPredicate<T, T> eq);
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
        return new ResumableList<>(() -> new ResPair<>(val, this));
    }

    @Override
    public ResumableList<T> join(ResumableList<T> l) {
        return new ResumableList<>(bind(s -> s.join(l)));
    }

    @Override
    public ResumableList<T> unique(List<T> hist, BiPredicate<T, T> eq) {
        return new ResumableList<>(bind(s -> s.unique(hist, eq)));
    }

    ResumableList<T> unique(BiPredicate<T, T> eq) {
        return unique(List.nul(), eq);
    }
}

abstract class ResState<T> implements ResInt<T> {
}

class ResPair<T> extends ResState<T> {
    final T val;
    final ResumableList<T> next;

    ResPair(T val, ResumableList<T> next) {
        this.val = val;
        this.next = next;
    }

    @Override
    public ResumableList<T> join(ResumableList<T> l) {
        return next.join(l).cons(val);
    }

    @Override
    public ResumableList<T> unique(List<T> hist, BiPredicate<T, T> eq) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'unique'");
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
        if (lstate instanceof ResPair) {
            return l.join(new ResumableList<>(() -> this));
        } else if (lstate instanceof Pause) {
            return new ResumableList<>(() -> new Pause<>(next.join(((Pause<T>) lstate).next)));
        } else {
            return new ResumableList<>(() -> this);
        }
    }

    @Override
    public ResumableList<T> unique(List<T> hist, BiPredicate<T, T> eq) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'unique'");
    }
}

class Null<T> extends ResState<T> {
    @Override
    public ResumableList<T> join(ResumableList<T> l) {
        return l;
    }

    @Override
    public ResumableList<T> unique(List<T> hist, BiPredicate<T, T> eq) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'unique'");
    }
}
