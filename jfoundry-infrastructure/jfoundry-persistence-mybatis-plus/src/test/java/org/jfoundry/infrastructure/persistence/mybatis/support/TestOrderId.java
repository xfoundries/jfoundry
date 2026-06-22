package org.jfoundry.infrastructure.persistence.mybatis.support;

import org.jmolecules.ddd.types.Identifier;

import java.io.Serializable;

/// 测试用聚合根 ID(强类型 Identifier)。
public record TestOrderId(String value) implements Identifier, Serializable {

    @Override
    public String toString() {
        return value;
    }
}
