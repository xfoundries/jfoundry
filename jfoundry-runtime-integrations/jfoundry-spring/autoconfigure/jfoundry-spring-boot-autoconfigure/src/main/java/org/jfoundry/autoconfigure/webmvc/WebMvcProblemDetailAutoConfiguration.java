package org.jfoundry.autoconfigure.webmvc;

import org.jfoundry.webmvc.spring.ProblemDetailExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for jfoundry Spring MVC ProblemDetail exception responses.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = {
        "org.springframework.web.servlet.DispatcherServlet",
        "org.jfoundry.webmvc.spring.ProblemDetailExceptionHandler"
})
public class WebMvcProblemDetailAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ProblemDetailExceptionHandler.class)
    public ProblemDetailExceptionHandler problemDetailExceptionHandler() {
        return new ProblemDetailExceptionHandler();
    }
}
