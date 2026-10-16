package com.cms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.entity.Tag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 标签 Mapper（含文章-标签关联操作）
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    @Insert("INSERT INTO cms_article_tag (article_id, tag_id) VALUES (#{articleId}, #{tagId})")
    int insertArticleTag(@Param("articleId") Long articleId, @Param("tagId") Long tagId);

    @Delete("DELETE FROM cms_article_tag WHERE article_id = #{articleId}")
    int deleteArticleTags(@Param("articleId") Long articleId);

    @Select("SELECT t.name FROM cms_tag t INNER JOIN cms_article_tag at ON at.tag_id = t.id WHERE at.article_id = #{articleId} ORDER BY t.id")
    List<String> selectTagNamesByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT COUNT(*) FROM cms_article_tag WHERE tag_id = #{tagId}")
    Long countArticleByTagId(@Param("tagId") Long tagId);
}
