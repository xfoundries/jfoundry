package org.jfoundry.infrastructure.persistence.mybatis;

import org.jfoundry.application.event.DomainEventContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MybatisPlusRepositoryApiTest {

    @Test
    void mybatisPlusRepositoryConstructorShouldNotExposeDomainEventContext() {
        assertThat(constructorParameterTypes(MybatisPlusRepository.class))
                .noneMatch(parameterTypes -> parameterTypes.contains(DomainEventContext.class));
    }

    private static List<List<Class<?>>> constructorParameterTypes(Class<?> type) {
        return List.of(type.getDeclaredConstructors()).stream()
                .map(Constructor::getParameterTypes)
                .map(List::of)
                .toList();
    }
}
