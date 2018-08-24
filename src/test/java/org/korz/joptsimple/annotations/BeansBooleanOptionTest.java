package org.korz.joptsimple.annotations;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class BeansBooleanOptionTest {
    interface Options {
        boolean isFoo();
        boolean getBar();
    }

    @Test
    public void test() {
        Options opts = Parser.parse(Options.class, "--foo", "--bar");
        assertThat(opts.isFoo(), is(true));
        assertThat(opts.getBar(), is(true));
    }

    @Test
    public void missing() {
        Options opts = Parser.parse(Options.class);
        assertThat(opts.isFoo(), is(false));
        assertThat(opts.getBar(), is(false));
    }
}
