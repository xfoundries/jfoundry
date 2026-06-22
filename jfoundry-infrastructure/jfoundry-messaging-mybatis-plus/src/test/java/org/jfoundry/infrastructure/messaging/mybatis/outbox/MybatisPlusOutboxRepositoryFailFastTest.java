package org.jfoundry.infrastructure.messaging.mybatis.outbox;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class MybatisPlusOutboxRepositoryFailFastTest {

    @Test
    void constructorFailsWhenPaginationInnerInterceptorMissing() {
        OutboxMapper mapper = mock(OutboxMapper.class);
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor(); // no inner interceptors

        assertThatThrownBy(() -> new MybatisPlusOutboxRepository(mapper, interceptor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PaginationInnerInterceptor");
    }
}
