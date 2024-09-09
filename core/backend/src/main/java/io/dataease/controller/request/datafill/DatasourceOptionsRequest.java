package io.dataease.controller.request.datafill;

import lombok.Getter;

@Getter
public class DatasourceOptionsRequest {

    private String optionTable;
    private String optionColumnKey;
    private String optionColumnValue;
    private String optionColumnOrder;

    private String optionOrder;
}
