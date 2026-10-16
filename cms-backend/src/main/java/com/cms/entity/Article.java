package com.cms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章实体（cms_article 表，v2.1 is_deleted 逻辑删除）
 */
@Data
@TableName("cms_article")
public class Article {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    /** 文章内容（HTML，入库前 JSoup 白名单清洗，见 7.8） */
    private String content;

    private String summary;

    private String coverImage;

    private Long categoryId;

    private Long authorId;

    /** 0-草稿 1-已发布 2-已下架 */
    private Integer status;

    private Boolean isTop;

    private Boolean isRecommend;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private String seoTitle;

    private String seoDescription;

    private String seoKeywords;

    private LocalDateTime publishTime;

    /** 逻辑删除（0-正常 1-删除，v2.1） */
    @TableLogic
    private Boolean isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
