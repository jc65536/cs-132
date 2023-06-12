import cs132.IR.token.*;

public class SVEnv {
    final String currFun;
    final int insCount;
    final List<T2<Identifier, Integer>> idOffsets;
    final boolean mainFun;

    SVEnv(String currFun, int insCount, List<T2<Identifier, Integer>> idOffsets, boolean firstFun) {
        this.currFun = currFun;
        this.insCount = insCount;
        this.idOffsets = idOffsets;
        this.mainFun = firstFun;
    }

    SVEnv() {
        this("", 0, List.nul(), false);
    }

    SVEnv inc() {
        return new SVEnv(currFun, insCount + 1, idOffsets, mainFun);
    }

    SVEnv setMainFun(boolean ff) {
        return new SVEnv(currFun, insCount, idOffsets, ff);
    }

    RVLabel mkLabel(Label label) {
        return new RVLabel(String.format("%s_%s", currFun, label.toString()));
    }

    RVLabel mkTempLabel() {
        return new RVLabel(String.format("%s_tmp_%d", currFun, insCount));
    }

    int offsetLookup(Identifier id) {
        return idOffsets.find(t -> t.a.toString().equals(id.toString())).get().b;
    }
}
