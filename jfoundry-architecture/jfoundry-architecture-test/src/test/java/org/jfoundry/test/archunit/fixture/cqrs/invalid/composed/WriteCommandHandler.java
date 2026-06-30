package org.jfoundry.test.archunit.fixture.cqrs.invalid.composed;

import org.jfoundry.architecture.cqrs.CommandHandler;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@CommandHandler
@Retention(RetentionPolicy.RUNTIME)
@interface WriteCommandHandler {
}
