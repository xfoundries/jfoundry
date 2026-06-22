package org.jfoundry.infrastructure.persistence.mybatis.support;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/// 测试用 TypeHandler:TestOrderId(record)↔ VARCHAR 互转。
/// <p>
/// 体现"业务侧用强类型 ID 时,需要为 MyBatis 提供 TypeHandler"的实践。
/// 真实业务场景中,framework 未来应提供通用 IdentifierTypeHandler 自动注册,避免每个 ID 类型都手写。
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(TestOrderId.class)
public class TestOrderIdTypeHandler extends BaseTypeHandler<TestOrderId> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, TestOrderId parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.value());
    }

    @Override
    public TestOrderId getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : new TestOrderId(value);
    }

    @Override
    public TestOrderId getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : new TestOrderId(value);
    }

    @Override
    public TestOrderId getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : new TestOrderId(value);
    }
}
