package org.jfoundry.infrastructure.persistence.mybatis;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import org.jfoundry.infrastructure.persistence.BaseData;
import org.jmolecules.ddd.types.Identifier;

import java.io.Serializable;
import java.time.LocalDateTime;

/// MyBatis Plus 可审计的数据类
/// 继承自 BaseData，添加审计相关字段
/// <p>
/// 这是 MyBatis-Plus 适配层提供的固定字段 convenience base class。非标准字段模型可以直接继承 BaseData，
/// 并在领域对象上实现 Auditable/Deletable 能力接口。
/// <p>
/// 主键字段 {@code id} 由父类 BaseData 提供；MyBatis-Plus 默认按字段名 {@code id} 识别为主键，
/// 因此无需在本类或父类上显式标注 {@code @TableId}。如需自定义主键策略（如 {@code IdType.AUTO}），
/// 业务子类可重新声明字段并标注 {@code @TableId}。
/// <p>
/// hashCode / equals / toString 直接继承父类实现（基于 ID 判断），
/// 与审计字段无关，避免两个未持久化对象因审计字段不同被错误折叠。
///
/// @param <ID> 标识符类型，必须是 jMolecules Identifier 且可序列化
public abstract class MybatisPlusAuditableData<ID extends Identifier & Serializable> extends BaseData<ID> {

    /// 创建人ID
    @TableField(fill = FieldFill.INSERT)
    private String creatorId;

    /// 创建人姓名
    @TableField(fill = FieldFill.INSERT)
    private String creatorName;

    /// 创建时间
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /// 最后修改人ID
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String lastModifierId;

    /// 最后修改人姓名
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String lastModifierName;

    /// 最后修改时间
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastModifiedTime;

    /// 是否已删除（软删除标记）
    @TableField(value = "is_deleted")
    @TableLogic
    private boolean deleted;

    /// 删除时间
    private LocalDateTime deletedTime;

    /// 删除人ID
    private String deleterId;

    /// 删除人姓名
    private String deleterName;

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public String getLastModifierId() {
        return lastModifierId;
    }

    public void setLastModifierId(String lastModifierId) {
        this.lastModifierId = lastModifierId;
    }

    public String getLastModifierName() {
        return lastModifierName;
    }

    public void setLastModifierName(String lastModifierName) {
        this.lastModifierName = lastModifierName;
    }

    public LocalDateTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(LocalDateTime lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getDeletedTime() {
        return deletedTime;
    }

    public void setDeletedTime(LocalDateTime deletedTime) {
        this.deletedTime = deletedTime;
    }

    public String getDeleterId() {
        return deleterId;
    }

    public void setDeleterId(String deleterId) {
        this.deleterId = deleterId;
    }

    public String getDeleterName() {
        return deleterName;
    }

    public void setDeleterName(String deleterName) {
        this.deleterName = deleterName;
    }
}
