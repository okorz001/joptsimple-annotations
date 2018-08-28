package org.korz.joptsimple.annotations;

/**
 * Called by a Parser during certain events.
 * @param <T> The arguments model.
 */
public interface ParserCallbacks<T> {
    /**
     * Called when the help option is parsed.
     * @param parser The parser that parsed the help option.
     */
    void onHelp(Parser<T> parser);

    /**
     * Called when an error occurs during parsing.
     * @param parser The parser that parsed the unknown option.
     * @param error The error message.
     */
    void onError(Parser<T> parser, String error);
}
