package io.github.lumine1909.cartography.processor;

import io.github.lumine1909.cartography.command.CommandContext;

public interface Processor<F, T> {

    T process(F from, CommandContext context);

    default <B> Processor<F, B> then(Processor<? super T, ? extends B> after) {
        return (f, context) -> after.process(this.process(f, context), context);
    }
}
