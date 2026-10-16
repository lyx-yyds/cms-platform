package com.cms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cms.dto.response.ArticleResp;
import com.cms.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 文章 Mapper（分页联查在 resources/mapper/ArticleMapper.xml）
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 文章分页联查（categoryName/authorName；tags 由服务端批量填充）
     */
    Page<ArticleResp> selectArticlePage(Page<ArticleResp> page,
                                        @Param("keyword") String keyword,
                                        @Param("categoryId") Long categoryId,
                                        @Param("status") Integer status,
                                        @Param("authorId") Long authorId);

    /**
     * 文章详情联查（含分类名/作者名）
     */
    ArticleResp selectArticleDetail(@Param("id") Long id);

    /** 浏览量增量落库（ViewCountFlushJob，文档 7.7.1） */
    @Update("UPDATE cms_article SET view_count = view_count + #{delta} WHERE id = #{id}")
    int updateViewCount(@Param("id") Long id, @Param("delta") long delta);

    /** 点赞数 +1（首次点赞，SADD 判定后） */
    @Update("UPDATE cms_article SET like_count = like_count + 1 WHERE id = #{id} AND is_deleted = 0")
    int increaseLikeCount(@Param("id") Long id);

    /** 点赞数 -1（取消点赞，SREM 判定后；GREATEST 防负值） */
    @Update("UPDATE cms_article SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{id} AND is_deleted = 0")
    int decreaseLikeCount(@Param("id") Long id);
}
