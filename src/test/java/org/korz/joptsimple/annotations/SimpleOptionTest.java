package org.korz.joptsimple.annotations;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class SimpleOptionTest {
    interface Options {
        int foo();
        String bar();
    }

    @Test
    public void test() {
        Options opts = Parser.parse(Options.class, "--foo", "1", "--bar", "2");
        assertThat(opts.foo(), is(1));
        assertThat(opts.bar(), is("2"));
    }

    @Test
    public void missing() {
        Options opts = Parser.parse(Options.class);
        assertThat(opts.foo(), is(0));
        assertThat(opts.bar(), nullValue());
    }
}
