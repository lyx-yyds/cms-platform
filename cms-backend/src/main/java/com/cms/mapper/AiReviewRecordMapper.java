package com.cms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.entity.AiReviewRecord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * AI 评论审核记录 Mapper
 */
@Mapper
public interface AiReviewRecordMapper extends BaseMapper<AiReviewRecord> {

    @Select("SELECT id, comment_id, content, review_result, score, model, status, create_time " +
            "FROM ai_review_record WHERE comment_id = #{commentId} ORDER BY id DESC LIMIT 1")
    AiReviewRecord selectLatestByCommentId(@Param("commentId") Long commentId);

    @Delete("DELETE FROM ai_review_record WHERE comment_id = #{commentId}")
    int deleteByCommentId(@Param("commentId") Long commentId);
}
