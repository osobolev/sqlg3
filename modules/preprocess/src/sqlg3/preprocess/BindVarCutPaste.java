package sqlg3.preprocess;

import java.util.List;

final class BindVarCutPaste extends CutPaste {

    private final String mainPart;
    private final List<ParamCutPaste> pieces;

    BindVarCutPaste(int from, int to, String mainPart, List<ParamCutPaste> pieces) {
        super(from, to);
        this.mainPart = mainPart;
        this.pieces = pieces;
    }

    @Override
    protected String getPasteText() {
        StringBuilder buf = new StringBuilder(mainPart);
        for (int i = pieces.size() - 1; i >= 0; i--) {
            pieces.get(i).cutPaste(buf);
        }
        return buf.toString();
    }
}
