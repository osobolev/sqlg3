package sqlg3.preprocess;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import sqlg3.preprocess.lexer.Java8Lexer;

import java.io.IOException;

abstract class ParserBase {

    protected final String text;
    private final Java8Lexer lexer;
    private Token token;

    protected ParserBase(String text) throws IOException {
        this.text = text;
        lexer = new Java8Lexer(CharStreams.fromString(text));
        token = lexer.nextToken();
    }

    protected final boolean eof() {
        return token.getType() == Java8Lexer.EOF;
    }

    protected final Token get() {
        return token;
    }

    protected final void next() {
        if (!eof()) {
            token = lexer.nextToken();
        }
    }
}
