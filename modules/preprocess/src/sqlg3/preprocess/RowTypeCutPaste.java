package sqlg3.preprocess;

final class RowTypeCutPaste extends CutPaste {

    final String className;

    String replaceTo = null;

    RowTypeCutPaste(int from, int to, String className) {
        super(from, to);
        this.className = className;
    }

    @Override
    protected String getPasteText() {
        return replaceTo;
    }

    public String toString() {
        return className;
    }
}
