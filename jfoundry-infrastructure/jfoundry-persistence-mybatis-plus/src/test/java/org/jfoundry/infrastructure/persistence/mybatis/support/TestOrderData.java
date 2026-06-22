package org.jfoundry.infrastructure.persistence.mybatis.support;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.jfoundry.infrastructure.persistence.BaseData;

import java.time.Instant;

/// 测试用 Data 对象(MyBatis-Plus 表映射)。
@TableName("test_order")
public class TestOrderData extends BaseData<TestOrderId> {

    /// 强类型 ID(TestOrderId record)需要 TypeHandler 桥接到 VARCHAR,TypeHandler 通过
    /// PersistenceTestConfig 的 ConfigurationCustomizer 全局注册(因 @TableId 不支持 typeHandler 属性)。
    @TableId(type = IdType.INPUT)
    private TestOrderId id;

    private String status;

    private Integer amount;

    private Instant createdAt;

    private Instant updatedAt;

    @Override
    public TestOrderId getId() {
        return id;
    }

    @Override
    public void setId(TestOrderId id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
