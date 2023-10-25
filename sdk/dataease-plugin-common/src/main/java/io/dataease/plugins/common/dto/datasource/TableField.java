package io.dataease.plugins.common.dto.datasource;

import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableField {
    private String fieldName;
    private String remarks;
    private String fieldType;
    private Integer fieldSize;
    private Integer accuracy;

}
