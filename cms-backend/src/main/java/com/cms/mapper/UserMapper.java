package com.cms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.entity.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /** 查询单个用户角色编码列表（RBAC：user → role） */
    @Select("SELECT r.code FROM sys_role r " +
            "JOIN sys_user_role ur ON ur.role_id = r.id " +
            "WHERE ur.user_id = #{userId} AND r.status = 1")
    List<String> selectRoleCodesByUserId(Long userId);

    /** 查询单个用户权限点编码列表（RBAC：user → role → permission） */
    @Select("SELECT DISTINCT p.code FROM sys_permission p " +
            "JOIN sys_role_permission rp ON rp.permission_id = p.id " +
            "JOIN sys_user_role ur ON ur.role_id = rp.role_id " +
            "JOIN sys_role r ON r.id = ur.role_id AND r.status = 1 " +
            "WHERE ur.user_id = #{userId} AND r.status = 1")
    List<String> selectPermissionCodesByUserId(Long userId);

    /** 批量查询用户角色编码（返回 userId/code 对，服务端分组） */
    @Select({"<script>",
            "SELECT ur.user_id AS userId, r.code AS code FROM sys_user_role ur ",
            "JOIN sys_role r ON r.id = ur.role_id AND r.status = 1 ",
            "WHERE ur.user_id IN ",
            "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"})
    List<Map<String, Object>> selectRoleCodesByUserIds(@Param("userIds") List<Long> userIds);

    /** 写入用户角色关联 */
    @Insert("INSERT INTO sys_user_role (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /** 删除用户全部角色关联 */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteUserRoles(Long userId);
}
