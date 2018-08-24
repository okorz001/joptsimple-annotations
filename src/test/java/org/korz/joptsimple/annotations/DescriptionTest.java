package org.korz.joptsimple.annotations;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class DescriptionTest {
    interface Options {
        int foo();
        @Description("doc")
        int bar();
    }

    @Test
    public void test() throws IOException {
        Parser<Options> parser = new Parser<>(Options.class);
        StringWriter writer = new StringWriter();
        parser.printHelp(writer);
        String[] lines = writer.toString().split("\n");
        for (String line : lines) {
            if (line.contains("--bar")) {
                assertThat(line, containsString("doc"));
                return;
            }
        }
        throw new AssertionError("could not find help for --bar");
    }
}
