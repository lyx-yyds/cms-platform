package com.cms.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章列表项（ArticleResp，XML 联查 categoryName/authorName，tags 由服务端填充）
 */
@Data
public class ArticleResp {

    private Long id;
    private String title;
    private String summary;
    private String coverImage;
    private Long categoryId;
    private String categoryName;
    private Long authorId;
    private String authorName;
    /** 0-草稿 1-已发布 2-已下架 */
    private Integer status;
    private Boolean isTop;
    private Boolean isRecommend;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
    private List<String> tags;
}
