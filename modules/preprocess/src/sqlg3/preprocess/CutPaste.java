package sqlg3.preprocess;

abstract class CutPaste {

    private final int from;
    private final int to;

    protected CutPaste(int from, int to) {
        this.from = from;
        this.to = to;
    }

    final void cutPaste(StringBuilder buf) {
        String pasteText = getPasteText();
        if (pasteText != null) {
            buf.delete(from, to);
            buf.insert(from, pasteText);
        }
    }

    protected abstract String getPasteText();
}
