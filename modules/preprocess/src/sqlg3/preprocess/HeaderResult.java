package sqlg3.preprocess;

final class HeaderResult {

    final String javadoc;
    final int spaceStart;
    final int brace;

    HeaderResult(String javadoc, int spaceStart, int brace) {
        this.javadoc = javadoc;
        this.spaceStart = spaceStart;
        this.brace = brace;
    }
}
