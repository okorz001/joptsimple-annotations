# joptsimple-annotations

The power of joptsimple with annotations instead of code.

## Goals

* Type safety: Parse arguments into models, making name and type mistakes compile-time errors.

* Simplicity: Reduce clutter and typing by assuming common cases.

* Flexibility: Annotations allow configuration of uncommon cases.

* Minimal: Defer to well-known language features when possible.

## Requirements

* Java 8
* jopt-simple

## Usage

The unit tests are always the most accurate documentation, but the following should get you off the ground.

The entry point for argument parsing is the `Parser` class. Parsing requires a model interface&mdash;possibly
annotated&mdash;and of course the arguments list.

```java
// Define your arguments model as a POJO interface. Note that concrete classes will not work.
interface Options {
    boolean verbose();
    int size();
}

// Create a Parser for this model.
Parser<Options> parser = new Parser<>(Options.class);

// Parse an arguments list.
String[] args = { "--verbose", "--size", "100" };
Options options = parser.parse(args);

assert options.verbose();
assert options.size() == 100;
```

If you do not need a reference to the `Parser` instance, there is a static convenience method that returns the
arguments directly.

```java
Options options = Parser.parse(args, Options.class);
```

### Help message

TODO: --help

### Describing options

Option names, types, and default values are automatically documented. An additional text description can be added with
`@Description`.

```java
interface Options {
    @Description("An optional size hint for efficient allocation")
    int size();
}
```

### Default values

Arguments that are not parsed will return their default values. For object types&mdash;including `String`&mdash;, this
is `null`. For primitives, this is generally either `0` or `false`. If `null` is desired instead, then use the boxed
type, e.g. `Integer` instead of `int`.

```java
// Define your arguments model as a POJO interface. Note that concrete classes will not work.
interface Options {
    boolean verbose();
    int size();
    Integer timeout();
}

// Parse an arguments list into model.
String[] args = {}; // empty
Options options = Parser.parse(args, Options.class);

assert !options.verbose();
assert options.size() == 0;
assert options.timeout() == null;
```

### Custom defaults

The default values can be overridden by implementing a default method for the argument.

```java
// Define your arguments model as a POJO interface. Note that concrete classes will not work.
interface Options {
    int size();
    default int timeout() {
        // This is arbitrary regular Java code. It does not have to be a constant value.
        return 60;
    }
}

// Parse an arguments list into model.
String[] args = {}; // empty
Options options = Parser.parse(args, Options.class);

assert options.size() == 0;
assert options.timeout() == 60;
```
