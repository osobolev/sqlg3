package sqlg3.preprocess;

final class HeaderResult {

    final int spaceStart;
    final int brace;

    HeaderResult(int spaceStart, int brace) {
        this.spaceStart = spaceStart;
        this.brace = brace;
    }
}
