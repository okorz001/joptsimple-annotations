package org.korz.joptsimple.annotations;

public class Example {
    interface Options {
        @Description("Enable verbose output")
        boolean verbose();
        @Description("Size hint for efficient allocations")
        default int size() {
            return 1024;
        }
    }

    public static void main(String[] args) {
        Options options = Parser.parse(Options.class, args);
        System.out.println("Parsed options: " + options);
    }
}
