package org.korz.joptsimple.annotations;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DefaultTest {
    public interface Options {
        default int foo() {
            return 42;
        }
    }

    @Test
    public void test() {
        Options opts = Parser.parse(Options.class);
        assertThat(opts.foo(), is(42));
    }

    @Test
    public void explicit() {
        Options opts = Parser.parse(Options.class, "--foo", "13");
        assertThat(opts.foo(), is(13));
    }
}
