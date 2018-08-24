package org.korz.joptsimple.annotations;

import org.junit.Test;

public class DuplicateOptionTest {
    interface Options {
        int foo();
        int getFoo();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test() {
        Options opts = Parser.parse(Options.class);
        // boom
    }
}
