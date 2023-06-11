import java.util.*;

public class SVEnv {
    final String currFun;
    final int insCount;
    final List<T2<String, Integer>> idOffsets;
    final boolean firstFun;

    SVEnv(String currFun, int insCount, List<T2<String, Integer>> idOffsets, boolean firstFun) {
        this.currFun = currFun;
        this.insCount = insCount;
        this.idOffsets = idOffsets;
        this.firstFun = firstFun;
    }

    SVEnv() {
        this("", 0, List.nul(), false);
    }

    SVEnv inc() {
        return new SVEnv(currFun, insCount + 1, idOffsets, firstFun);
    }

    SVEnv setFirstFun(boolean ff) {
        return new SVEnv(currFun, insCount, idOffsets, ff);
    }

    RVLabel mkLabel(String name) {
        return new RVLabel(String.format("%s_%s", currFun, name));
    }

    RVLabel mkTempLabel(String name) {
        return new RVLabel(String.format("%s_tmp_%d", currFun, insCount));
    }

    int offsetLookup(String id) {
        return idOffsets.find(t -> t.a.equals(id)).get().b;
    }
}
