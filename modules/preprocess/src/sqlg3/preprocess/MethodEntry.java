package sqlg3.preprocess;

import java.util.List;

final class MethodEntry {

    final String javadoc;
    final String methodToCall;
    final List<String> paramNames;
    final boolean publish;

    MethodEntry(String javadoc, String methodToCall, List<String> paramNames, boolean publish) {
        this.javadoc = javadoc;
        this.methodToCall = methodToCall;
        this.paramNames = paramNames;
        this.publish = publish;
    }
}
