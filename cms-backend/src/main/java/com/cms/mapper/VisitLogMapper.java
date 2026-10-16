package com.cms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.entity.VisitLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 访问日志 Mapper（文档 7.11 统计口径）
 */
@Mapper
public interface VisitLogMapper extends BaseMapper<VisitLog> {

    /** PV/UV 按日聚合：维度 time（YYYY-MM-DD） */
    List<Map<String, Object>> selectPvUvByDay(java.time.LocalDate start);

    /** PV/UV 按月聚合：维度 time（YYYY-MM） */
    List<Map<String, Object>> selectPvUvByMonth(java.time.LocalDate start);

    /** 按日新增用户数 */
    List<Map<String, Object>> selectUserGrowthByDay(java.time.LocalDate start);

    /** 按月新增用户数 */
    List<Map<String, Object>> selectUserGrowthByMonth(java.time.LocalDate start);

    /** 角色分布统计 */
    List<Map<String, Object>> selectRoleDistribution();

    /** 文章状态分布（0草稿/1发布/2下架） */
    List<Map<String, Object>> selectArticleStatusCount();
}
