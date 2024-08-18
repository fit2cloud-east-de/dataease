package io.dataease.controller.request.datasource;

import lombok.Data;

/**
 * @program: dataease
 * @description:
 * @author: wanziliang
 * @create: 2024-06-11 17:52
 **/
@Data
public class OpcUaDefinitionRequest {

    private String endpoint;
    private String opcUaUsername;
    private String opcUaPassword;

}
