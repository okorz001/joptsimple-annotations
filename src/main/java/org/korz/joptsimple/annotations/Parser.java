package org.korz.joptsimple.annotations;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.lang.reflect.Proxy.newProxyInstance;

/**
 * Parses program arguments into a strongly typed model.
 * @param <T> The arguments model type.
 */
public class Parser<T> {
    /**
     * Parses arguments into a strongly typed model.
     * @param modelClass The arguments model class.
     * @param args The arguments.
     * @param <T> The arguments model type.
     * @return The parsed arguments.
     * @throws NullPointerException If modelClass or args are null.
     * @throws IllegalArgumentException if modelClass is not an interface.
     */
    public static <T> T parse(Class<T> modelClass, List<String> args) {
        return newParser(modelClass)
            .build()
            .parse(args);
    }

    /**
     * Parses arguments into a strongly typed model.
     * @param modelClass The arguments model class.
     * @param args The arguments.
     * @param <T> The arguments model type.
     * @return The parsed arguments.
     * @throws NullPointerException If modelClass or args are null.
     * @throws IllegalArgumentException if modelClass is not an interface.
     */
    public static <T> T parse(Class<T> modelClass, String... args) {
        return newParser(modelClass)
            .build()
            .parse(args);
    }

    /**
     * Creates a new builder for Parser instances.
     * @param modelClass The arguments model class.
     * @param <T> The arguments model type.
     * @return A new builder.
     * @throws NullPointerException If modelClass is null.
     * @throws IllegalArgumentException if modelClass is not an interface.
     */
    public static <T> Builder<T> newParser(Class<T> modelClass) {
        return new Builder<>(modelClass);
    }

    /**
     * Constructs a new Parser instance.
     * @param <T> The arguments model type.
     */
    public static class Builder<T> {
        private final Class<T> modelClass;
        private ParserCallbacks<T> parserCallbacks = new DefaultParserCallbacks<>();

        private Builder(Class<T> modelClass) {
            if (modelClass == null) {
                throw new NullPointerException("modelClass is null");
            }
            if (!modelClass.isInterface()) {
                throw new IllegalArgumentException("modelClass is not an interface: " + modelClass.getName());
            }
            this.modelClass = modelClass;
        }

        /**
         * Sets the callbacks for the Parser instance.
         * @param parserCallbacks The callbacks.
         * @return This builder.
         * @throws NullPointerException If parserCallbacks is null.
         */
        public Builder<T> withCallbacks(ParserCallbacks<T> parserCallbacks) {
            if (parserCallbacks == null) {
                throw new NullPointerException("parserCallbacks is null");
            }
            this.parserCallbacks = parserCallbacks;
            return this;
        }

        /**
         * Constructs a new Parser instance.
         * @return A new Parser instance.
         */
        public Parser<T> build() {
            return new Parser<>(this);
        }
    }

    private final Class<T> modelClass;
    private final OptionParser parser;
    private final String helpOption = "help";
    private final ParserCallbacks<T> parserCallbacks;

    private Parser(Builder<T> builder) {
        modelClass = builder.modelClass;
        parserCallbacks = builder.parserCallbacks;
        parser = createParser(modelClass);
    }

    public void printHelp(Writer out) {
        try {
            parser.printHelpOn(out);
        } catch (IOException e) {
            throw new UncheckedIOException("printHelpOn failed", e);
        }
    }

    public void printHelp(OutputStream out) {
        try {
            parser.printHelpOn(out);
        } catch (IOException e) {
            throw new UncheckedIOException("printHelpOn failed", e);
        }
    }

    /**
     * Parses arguments into a strongly typed model.
     * @param args The arguments.
     * @return The parsed arguments.
     * @throws NullPointerException If args is null.
     */
    public T parse(List<String> args) {
        if (args == null) {
            throw new NullPointerException("args is null");
        }
        return parse(args.toArray(new String[0]));
    }

    /**
     * Parses arguments into a strongly typed model.
     * @param args The arguments.
     * @return The parsed arguments.
     * @throws NullPointerException If args is null.
     * @throws IllegalArgumentException If the parser callback for unknown options does not terminate.
     */
    public T parse(String... args) {
        if (args == null) {
            throw new NullPointerException("args is null");
        }
        OptionSet optionSet;
        try {
            optionSet = parser.parse(args);
        } catch (OptionException e) {
            parserCallbacks.onError(this, e.getMessage());
            throw new IllegalArgumentException("invalid arguments", e);
        }
        if (optionSet.has(helpOption)) {
            parserCallbacks.onHelp(this);
        }
        @SuppressWarnings("unchecked") // newProxyInstance is guaranteed to return a T
        T model = (T) newProxyInstance(modelClass.getClassLoader(),
                                       new Class[] { modelClass },
                                       new OptionsInvocationHandler(modelClass, optionSet));
        return model;
    }

    private static OptionParser createParser(Class<?> modelClass) {
        OptionParser parser = new OptionParser();
        boolean addDefaultHelp = true;
        for (Method method : modelClass.getMethods()) {
            String name = getOptionName(method);
            if (parser.recognizedOptions().containsKey(name)) {
                throw new IllegalArgumentException("Class has multiple definitions of option: " + name);
            }
            String description = getAnnotation(method, Description.class)
                .map(Description::value)
                .orElse("");
            if (isOptionFlag(method)) {
                // Flags have no arguments
                parser.accepts(name, description);
            } else {
                // TODO: generic type is clunky here with reflection
                ArgumentAcceptingOptionSpec optionSpec = parser.accepts(name, description).withRequiredArg();
                // TODO: support converters
                optionSpec.ofType(method.getReturnType());
                if (method.getReturnType().isPrimitive()) {
                    // Prevent NPE by setting defaults for primitive types
                    optionSpec.defaultsTo(getDefaultValue(method.getReturnType()));
                }
            }
        }
        if (addDefaultHelp) {
            parser.accepts("help", "Show this help")
                .forHelp();
        }
        return parser;
    }

    private static String getOptionName(Method method) {
        String name = method.getName();
        if (isOptionFlag(method) && name.startsWith("is")) {
            return unprefixAndLowerFirst("is", name);
        } else if (name.startsWith("get")) {
            return unprefixAndLowerFirst("get", name);
        }
        return name;
    }

    private static boolean isOptionFlag(Method method) {
        return method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class;
    }

    private static String unprefixAndLowerFirst(String prefix, String string) {
        int index = prefix.length();
        return Character.toLowerCase(string.charAt(index)) + string.substring(index + 1);
    }


    private static <T extends Annotation> Optional<T> getAnnotation(Method method, Class<T> annotationClass) {
        T[] annotations = method.getAnnotationsByType(annotationClass);
        return annotations.length == 0 ? Optional.empty() : Optional.of(annotations[0]);
    }

    private static Object getDefaultValue(Class<?> valueType) {
        if (valueType == byte.class) {
            return (byte) 0;
        } else if (valueType == short.class) {
            return (short) 0;
        } else if (valueType == int.class) {
            return 0;
        } else if (valueType == long.class) {
            return 0L;
        } else if (valueType == char.class) {
            return (char) 0;
        } else if (valueType == float.class) {
            return 0F;
        } else if (valueType == double.class) {
            return 0D;
        } else {
            throw new RuntimeException("Unknown primitive type: " + valueType);
        }
    }

    // hacks to deal with poor and inconsistent API
    private static Lookup getLookup(Class<?> modelClass) {
        try {
            Constructor<Lookup> c = Lookup.class.getDeclaredConstructor(Class.class);
            if (!c.isAccessible()) {
                c.setAccessible(true);
            }
            return c.newInstance(modelClass);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("failed to create Lookup with private access", e);
        }
    }

    private static class OptionsInvocationHandler implements InvocationHandler {
        final Class<?> modelClass;
        final OptionSet options;
        final Lookup lookup;

        OptionsInvocationHandler(Class<?> modelClass, OptionSet options) {
            this.modelClass = modelClass;
            this.options = options;
            lookup = getLookup(modelClass);
        }

        @Override // InvocationHandler
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = getOptionName(method);
            if (isOptionFlag(method)) {
                return options.has(name);
            }
            if (!options.has(name) && method.isDefault()) {
                try {
                    return lookup
                        .unreflectSpecial(method, modelClass)
                        .bindTo(proxy)
                        .invokeWithArguments(args);
                } catch (Throwable e) {
                    throw new RuntimeException("failed to invoke default method", e);
                }
            }
            return options.valueOf(name);
        }
    }
}
