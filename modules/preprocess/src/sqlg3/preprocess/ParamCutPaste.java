package sqlg3.preprocess;

final class ParamCutPaste extends CutPaste {

    final String param;
    final boolean out;
    String replaceTo = null;

    ParamCutPaste(int from, int to, String param, boolean out) {
        super(from, to);
        this.param = param;
        this.out = out;
    }

    @Override
    protected String getPasteText() {
        return replaceTo;
    }
}
