package sqlg3.preprocess;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

final class ParseResult {

    final Path file;
    final String text;
    final HeaderResult header;
    final List<MethodEntry> entries;
    final Map<ParamName, List<ParamCutPaste>> bindMap;
    final List<ParamName> parameters;
    private final List<CutPaste> fragments;

    ParseResult(Path file, String text, HeaderResult header, List<MethodEntry> entries, Map<ParamName, List<ParamCutPaste>> bindMap, List<ParamName> parameters, List<CutPaste> fragments) {
        this.file = file;
        this.text = text;
        this.header = header;
        this.entries = entries;
        this.bindMap = bindMap;
        this.parameters = parameters;
        this.fragments = fragments;
    }

    String doCutPaste() {
        StringBuilder buf = new StringBuilder(text);
        for (int i = fragments.size() - 1; i >= 0; i--) {
            CutPaste cp = fragments.get(i);
            cp.cutPaste(buf);
        }
        return buf.toString();
    }

    void insertIfaceFragment(IfaceCutPaste cp) {
        fragments.add(0, cp);
    }
}
