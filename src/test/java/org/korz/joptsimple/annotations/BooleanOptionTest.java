package org.korz.joptsimple.annotations;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class BooleanOptionTest {
    interface Options {
        boolean foo();
    }

    @Test
    public void test() {
        Options opts = Parser.parse(Options.class, "--foo");
        assertThat(opts.foo(), is(true));
    }

    @Test
    public void missing() {
        Options opts = Parser.parse(Options.class);
        assertThat(opts.foo(), is(false));
    }
}
