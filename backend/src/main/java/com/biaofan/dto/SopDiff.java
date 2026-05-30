package com.biaofan.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * SOP 版本差异 DTO
 */
@Data
@Builder
public class SopDiff {
    private Integer v1;
    private Integer v2;
    private List<StepDiff> steps;
    private PropertyDiff title;
    private PropertyDiff description;

    @Data
    @Builder
    public static class StepDiff {
        private String type; // added, removed, modified, unchanged
        private Integer oldIndex;
        private Integer newIndex;
        private PropertyDiff title;
        private PropertyDiff description;
        private List<String> addedCheckItems;
        private List<String> removedCheckItems;
    }

    @Data
    @Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class PropertyDiff {
        private String oldVal;
        private String newVal;
        private Boolean changed;
    }
}
