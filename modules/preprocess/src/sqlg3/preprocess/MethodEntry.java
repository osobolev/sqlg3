package sqlg3.preprocess;

final class MethodEntry {

    final String javadoc;
    final String methodToCall;
    final boolean publish;

    MethodEntry(String javadoc, String methodToCall, boolean publish) {
        this.javadoc = javadoc;
        this.methodToCall = methodToCall;
        this.publish = publish;
    }
}
