package org.jfoundry.test.archunit.fixture.cqrs.valid.application.composed;

import org.jfoundry.architecture.cqrs.CommandDispatcher;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@CommandDispatcher
@Retention(RetentionPolicy.RUNTIME)
@interface WriteCommandDispatcher {
}
