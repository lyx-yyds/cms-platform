package com.cms.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 人工复核请求（CommentAuditReq：管理员覆盖机审结果，文档 5.1/7.5）
 */
@Data
public class CommentAuditReq {

    /** 审核结果：1-通过 2-拒绝 */
    @NotNull(message = "审核结果不能为空")
    @Min(value = 1, message = "审核结果取值 1-通过 2-拒绝")
    @Max(value = 2, message = "审核结果取值 1-通过 2-拒绝")
    private Integer status;
}
