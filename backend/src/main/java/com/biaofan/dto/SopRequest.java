package com.biaofan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

/**
 * SOP（标准作业程序）请求参数封装
 * <p>用于创建或更新SOP文档，包含标题、描述、内容步骤、分类、标签和状态等信息</p>
 */
@Data
public class SopRequest {
    /** SOP标题 */
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200")
    private String title;
    /** SOP描述说明 */
    private String description;
    /** SOP内容步骤列表 */
    private List<StepItem> content;
    /** SOP所属分类 */
    @NotBlank(message = "分类不能为空")
    private String category;
    /** SOP标签列表，用于搜索和筛选 */
    private List<String> tags;
    /** SOP状态：draft-草稿，published-已发布 */
    private String status; // draft / published
    /** 发布时的变更摘要说明 */
    private String changeSummary; // for publish with summary

    /**
     * SOP步骤项
     * <p>定义SOP中的单个步骤，包含标题、描述和预计时长</p>
     */
    @Data
    public static class StepItem {
        /** 步骤标题 */
        private String title;
        /** 步骤详细描述 */
        private String description;
        /** 步骤预计时长（分钟） */
        private Integer duration;
    }
}
