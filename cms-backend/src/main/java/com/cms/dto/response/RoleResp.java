package com.cms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 角色列表项（含权限编码，RoleResp）
 */
@Data
@Builder
public class RoleResp {

    private Long id;
    private String name;
    private String code;
    private String description;
    private Integer status;
    private List<String> permissions;
}
