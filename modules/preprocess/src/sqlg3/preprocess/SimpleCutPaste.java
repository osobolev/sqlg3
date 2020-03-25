package sqlg3.preprocess;

final class SimpleCutPaste extends CutPaste {

    private final String replaceTo;

    SimpleCutPaste(int from, int to, String replaceTo) {
        super(from, to);
        this.replaceTo = replaceTo;
    }

    protected String getPasteText() {
        return replaceTo;
    }
}
