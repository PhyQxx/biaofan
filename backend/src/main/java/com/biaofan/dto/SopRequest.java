package com.biaofan.dto;

import lombok.Data;
import java.util.List;

@Data
public class SopRequest {
    private String title;
    private String description;
    private List<StepItem> content;
    private String category;
    private List<String> tags;
    private String status; // draft / published

    @Data
    public static class StepItem {
        private String title;
        private String description;
        private Integer duration;
    }
}
