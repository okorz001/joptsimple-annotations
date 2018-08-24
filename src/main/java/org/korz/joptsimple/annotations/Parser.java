package org.korz.joptsimple.annotations;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static java.lang.reflect.Proxy.newProxyInstance;

public class Parser<T> {
    public static <T> T parse(List<String> args, Class<T> modelClass) {
        return new Parser<>(modelClass).parse(args);
    }

    public static <T> T parse(String[] args, Class<T> modelClass) {
        return new Parser<>(modelClass).parse(args);
    }

    public static <T> T parse(Class<T> modelClass, String... args) {
        return parse(args, modelClass);
    }

    private final Class<T> modelClass;
    private final OptionParser parser;

    public Parser(Class<T> modelClass) {
        if (modelClass == null) {
            throw new NullPointerException("modelClass is null");
        }
        if (!modelClass.isInterface()) {
            throw new IllegalArgumentException("modelClass is not an interface: " + modelClass.getName());
        }
        this.modelClass = modelClass;
        parser = newParser(modelClass);
    }

    public void printHelp(Writer out) throws IOException {
        parser.printHelpOn(out);
    }

    public void printHelp(OutputStream out) throws IOException {
        parser.printHelpOn(out);
    }

    public T parse(List<String> args) {
        if (args == null) {
            throw new NullPointerException("args is null");
        }
        return parse(args.toArray(new String[0]));
    }

    public T parse(String... args) {
        if (args == null) {
            throw new NullPointerException("args is null");
        }
        @SuppressWarnings("unchecked") // newProxyInstance is guaranteed to return a T
        T model = (T) newProxyInstance(modelClass.getClassLoader(),
                                       new Class[] { modelClass },
                                       new OptionsInvocationHandler(parser.parse(args)));
        return model;
    }

    private static OptionParser newParser(Class<?> modelClass) {
        OptionParser parser = new OptionParser();
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

    private static class OptionsInvocationHandler implements InvocationHandler {
        final OptionSet options;

        OptionsInvocationHandler(OptionSet options) {
            this.options = options;
        }

        @Override // InvocationHandler
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = getOptionName(method);
            if (isOptionFlag(method)) {
                return options.has(name);
            }
            return options.valueOf(name);
        }
    }
}
