package com.cms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限实体（sys_permission 表）
 */
@Data
@TableName("sys_permission")
public class Permission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 权限编码（如 article:create） */
    private String code;

    /** 1-菜单 2-按钮 3-接口 */
    private Integer type;

    private Long parentId;

    private String path;

    private String icon;

    private Integer sortOrder;

    private LocalDateTime createTime;
}
