package io.quarkus.qute;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import io.quarkus.arc.Arc;
import io.quarkus.qute.runtime.TemplateProducer;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

class Controller {
    static class foo implements View {

        private final String str;
        private final int i;
        // added
        private final TemplateInstance wrapped;

        public foo(String str, int i) {
            this.str = str;
            this.i = i;
            Template template = Arc.container().instance(TemplateProducer.class).get().getInjectableTemplate("Controller/foo");
            TemplateInstance instance = template.instance();
            instance = instance.data("str", str);
            instance = instance.data("i", i);
            this.wrapped = instance;
        }

        @Override
        public TemplateInstance wrapped() {
            return wrapped;
        }
    }
}

public interface View extends TemplateInstance {

    public default TemplateInstance wrapped() {
        throw new RuntimeException("This should be generated");
    }

    @Override
    public default TemplateInstance data(Object data) {
        return wrapped().data(data);
    }

    @Override
    public default TemplateInstance data(String key, Object data) {
        return wrapped().data(key, data);
    }

    @Override
    public default TemplateInstance setAttribute(String key, Object value) {
        return wrapped().setAttribute(key, value);
    }

    @Override
    public default Object getAttribute(String key) {
        return wrapped().getAttribute(key);
    }

    @Override
    public default String render() {
        return wrapped().render();
    }

    @Override
    public default CompletionStage<String> renderAsync() {
        return wrapped().renderAsync();
    }

    @Override
    public default Multi<String> createMulti() {
        return wrapped().createMulti();
    }

    @Override
    public default Uni<String> createUni() {
        return wrapped().createUni();
    }

    @Override
    public default CompletionStage<Void> consume(Consumer<String> consumer) {
        return wrapped().consume(consumer);
    }

    @Override
    public default long getTimeout() {
        return wrapped().getTimeout();
    }

    @Override
    public default Template getTemplate() {
        return wrapped().getTemplate();
    }

    @Override
    public default TemplateInstance onRendered(Runnable action) {
        return wrapped().onRendered(action);
    }
}
