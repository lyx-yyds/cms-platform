package com.cms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体（sys_role 表）
 */
@Data
@TableName("sys_role")
public class Role {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 角色编码（如 ADMIN） */
    private String code;

    private String description;

    /** 1-正常 0-禁用 */
    private Integer status;

    private LocalDateTime createTime;
}
