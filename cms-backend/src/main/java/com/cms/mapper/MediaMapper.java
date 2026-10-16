package com.cms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cms.dto.response.MediaResp;
import com.cms.entity.Media;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 媒体文件 Mapper
 */
@Mapper
public interface MediaMapper extends BaseMapper<Media> {

    /** 媒体分页（联查上传者昵称，文档 5.1） */
    Page<MediaResp> selectMediaPage(Page<MediaResp> page,
                                    @Param("type") String type,
                                    @Param("keyword") String keyword);
}
