package com.cms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cms.dto.response.CommentResp;
import com.cms.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 评论 Mapper（分页联查在 resources/mapper/CommentMapper.xml）
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 评论分页联查（文章标题/评论者昵称/被回复者昵称；ai 明细由服务端补充）
     */
    Page<CommentResp> selectCommentPage(Page<CommentResp> page,
                                        @Param("articleId") Long articleId,
                                        @Param("status") Integer status,
                                        @Param("aiStatus") Integer aiStatus,
                                        @Param("keyword") String keyword);

    /**
     * 文章评论列表（仅已通过，公开）
     */
    java.util.List<CommentResp> selectApprovedByArticle(@Param("articleId") Long articleId);
}
