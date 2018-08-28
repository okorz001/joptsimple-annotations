package org.korz.joptsimple.annotations;

public class DefaultParserCallbacks<T> implements ParserCallbacks<T> {
    @Override
    public void onHelp(Parser<T> parser) {
        parser.printHelp(System.out);
        System.exit(0);
    }

    @Override
    public void onError(Parser<T> parser, String error) {
        System.err.println("Error: " + error);
        parser.printHelp(System.err);
        System.exit(1);
    }
}
