package org.korz.joptsimple.annotations;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class BeansOptionTest {
    interface Options {
        int getFoo();
        String getBar();
    }

    @Test
    public void test() {
        Options opts = Parser.parse(Options.class, "--foo", "1", "--bar", "2");
        assertThat(opts.getFoo(), is(1));
        assertThat(opts.getBar(), is("2"));
    }
}
