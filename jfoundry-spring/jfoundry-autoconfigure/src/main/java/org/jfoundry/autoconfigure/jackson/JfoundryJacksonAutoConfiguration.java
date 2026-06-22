package org.jfoundry.autoconfigure.jackson;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmolecules.jackson.JMoleculesModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/// 自动注册 jmolecules Jackson Module，让 ValueObject / Identifier 类型以单值形式
/// 序列化（而不是包裹成 JSON 对象）。
/// <p>
/// 仅当 classpath 上同时有 {@link ObjectMapper} 和 {@link JMoleculesModule} 时生效；
/// 业务侧若自定义了同类型 Module Bean，本自动注册退让。
@AutoConfiguration
@ConditionalOnClass({ObjectMapper.class, JMoleculesModule.class})
@ConditionalOnBean(ObjectMapper.class)
public class JfoundryJacksonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Module.class)
    public JMoleculesModule jmoleculesJacksonModule() {
        return new JMoleculesModule();
    }
}
