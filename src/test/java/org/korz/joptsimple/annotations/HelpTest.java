package org.korz.joptsimple.annotations;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class HelpTest {

    interface Options {
        // unused
    }

    boolean callback = false;

    @Test
    public void test() {
        Parser.newParser(Options.class)
            .withCallbacks(new DefaultParserCallbacks<Options>() {
                @Override
                public void onHelp(Parser<Options> parser) {
                    callback = true;
                }
            })
            .build()
            .parse("--help");
        assertThat(callback, is(true));
    }
}
