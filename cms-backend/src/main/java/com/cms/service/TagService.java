package com.cms.service;

import com.cms.dto.request.TagReq;
import com.cms.dto.response.TagResp;

import java.util.List;

/**
 * 标签服务
 */
public interface TagService {

    List<TagResp> listTags();

    void createTag(TagReq req);

    void updateTag(Long id, TagReq req);

    /** 删除标签（存在文章关联时拒绝） */
    void deleteTag(Long id);
}
