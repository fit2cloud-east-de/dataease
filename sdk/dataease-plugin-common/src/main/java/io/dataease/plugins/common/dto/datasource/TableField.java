package io.dataease.plugins.common.dto.datasource;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;

@Setter
@Getter
public class TableField {

    private String nodeId;
    private String fieldName;
    private String remarks;
    private String fieldType;
    private int fieldSize;
    private int accuracy;
    private boolean notNull;
    private boolean primaryKey;

    private Field field;

}
