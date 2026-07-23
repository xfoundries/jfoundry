package org.jfoundry.autoconfigure.webmvc;

import org.jfoundry.webmvc.spring.ProblemDetailExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class WebMvcProblemDetailAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebMvcProblemDetailAutoConfiguration.class));

    @Test
    void createsProblemDetailExceptionHandler() {
        runner.run(context -> assertThat(context).hasSingleBean(ProblemDetailExceptionHandler.class));
    }

    @Test
    void backsOffWhenUserProvidesExceptionHandler() {
        ProblemDetailExceptionHandler userHandler = new ProblemDetailExceptionHandler();

        runner.withBean(ProblemDetailExceptionHandler.class, () -> userHandler)
                .run(context -> {
                    assertThat(context).hasSingleBean(ProblemDetailExceptionHandler.class);
                    assertThat(context.getBean(ProblemDetailExceptionHandler.class)).isSameAs(userHandler);
                });
    }
}
