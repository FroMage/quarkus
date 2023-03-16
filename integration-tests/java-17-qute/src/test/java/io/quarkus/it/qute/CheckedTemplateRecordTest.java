package io.quarkus.it.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.View;
import io.quarkus.test.QuarkusUnitTest;

public class CheckedTemplateRecordTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(foo.class)
                    .addAsResource(new StringAsset("Hello {str} {i}!"),
                            "templates/CheckedTemplateRecordTest/foo.txt"));

    @Test
    public void testPrimitiveParamBinding() {
        assertEquals("Hello Stef 1!", new foo("Stef", 1).render());
    }

    public record foo(String str, int i) implements View {
    }
}
