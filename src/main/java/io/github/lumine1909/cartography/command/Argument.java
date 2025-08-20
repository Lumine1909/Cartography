package io.github.lumine1909.cartography.command;

import io.github.lumine1909.cartography.util.PackMode;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public record Argument(
    String url, boolean useDithering, int length, int width,
    boolean keepScale, PackMode pack, boolean noCache
) {

    private static Argument of(String url, Boolean useDithering, Integer width, Integer height, Boolean reshape, PackMode pack, Boolean noCache) {
        return new Argument(
            orThrow(url), or(useDithering, false), positiveOr(width, 1), positiveOr(height, 1),
            or(reshape, false), or(pack, PackMode.NORMAL), or(noCache, false)
        );
    }

    private record ArgumentParser<T>(int index, int check, Function<String, T> parser, List<String> completion) {

        public static <T> ArgumentParser<T> of(int index, int check, Function<String, T> parser, String... completion) {
            return new ArgumentParser<>(index, check, parser, List.of(completion));
        }

        T parse(String input) {
            return parser.apply(input);
        }
    }

    private static final Map<String, ArgumentParser<?>> PARSERS = Map.of(
        "-url", ArgumentParser.of(0, 1, Function.identity(), "<url>"),
        "-dither", ArgumentParser.of(1, 0, str -> true),
        "-l", ArgumentParser.of(2, 1, Argument::toInt, "1", "3"),
        "-w", ArgumentParser.of(3, 1, Argument::toInt, "1", "3"),
        "-keepscale", ArgumentParser.of(4, 0, str -> true),
        "-pack", ArgumentParser.of(5, 1, PackMode::get, PackMode.values),
        "-nocache", ArgumentParser.of(6, 0, str -> true)
    );

    public static Argument fromCommandArgs(String[] args) {
        Object[] arguments = new Object[PARSERS.size()];
        for (int i = 0; i < args.length; i++) {
            var parser = PARSERS.get(args[i]);
            if (parser == null) {
                throw new IllegalArgumentException("Failed to find argument: " + args[i]);
            }
            i += parser.check();
            if (i >= args.length) {
                break;
            }
            arguments[parser.index] = parser.parse(args[i]);
        }
        return Argument.of(
            (String) arguments[0], (Boolean) arguments[1], (Integer) arguments[2], (Integer) arguments[3],
            (Boolean) arguments[4], (PackMode) arguments[5], (Boolean) arguments[6]
        );
    }

    public static List<String> getCompletion(String[] args) {
        String last = args.length >= 2 ? args[args.length - 2] : "";
        var parser = PARSERS.get(last);
        List<String> completion;
        if (parser != null && !(completion = parser.completion()).isEmpty()) {
            return completion;
        }
        Set<String> alreadyArgs = new HashSet<>();
        for (String str : args) {
            if (PARSERS.containsKey(str)) {
                alreadyArgs.add(str);
            }
        }
        return PARSERS.keySet().stream().filter(str -> !alreadyArgs.contains(str)).toList();
    }

    private static int toInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T or(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static <T> T positiveOr(T value, T defaultValue) {
        return (value instanceof Number number && number.intValue() > 0) ? value : defaultValue;
    }

    private static <T> T orThrow(T value) {
        if (value == null) {
            throw new IllegalArgumentException("Invalid argument: null");
        }
        return value;
    }
}
