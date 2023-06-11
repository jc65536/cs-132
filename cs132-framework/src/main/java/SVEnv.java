import java.util.*;

public class SVEnv {
    final String currFun;
    final int insCount;
    final List<T2<String, Integer>> idOffsets;

    SVEnv(String currFun, int insCount, List<T2<String, Integer>> idOffsets) {
        this.currFun = currFun;
        this.insCount = insCount;
        this.idOffsets = idOffsets;
    }

    SVEnv inc() {
        return new SVEnv(currFun, insCount + 1, idOffsets);
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
