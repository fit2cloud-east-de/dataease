package io.dataease.api.ds.vo;

import lombok.Data;

import java.util.List;

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
    private List<String> nodeList;
    private String connectionType;

}
