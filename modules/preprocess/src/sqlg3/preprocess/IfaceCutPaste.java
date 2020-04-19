package sqlg3.preprocess;

final class IfaceCutPaste extends CutPaste {

    private final String text;
    String replaceTo = " ";

    IfaceCutPaste(int from, int to, String text) {
        super(from, to);
        this.text = text;
    }

    protected String getPasteText() {
        return replaceTo;
    }

    static String getImplements(String ifaceClassName, boolean add) {
        if (add) {
            return ", " + ifaceClassName;
        } else {
            return " implements " + ifaceClassName;
        }
    }

    static IfaceCutPaste create(String text, HeaderResult header, String ifaceClassName) {
        String[] tries = {getImplements(ifaceClassName, false), getImplements(ifaceClassName, true)};
        int from = header.spaceStart;
        for (String str : tries) {
            int ifrom = header.spaceStart - str.length();
            if (text.regionMatches(ifrom, str, 0, str.length())) {
                from = ifrom;
                break;
            }
        }
        return new IfaceCutPaste(from, header.brace, text);
    }

    String removeIface() {
        StringBuilder buf = new StringBuilder(text);
        cutPaste(buf);
        return buf.toString();
    }
}
