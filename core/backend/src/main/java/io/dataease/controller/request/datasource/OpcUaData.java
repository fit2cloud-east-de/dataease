package io.dataease.controller.request.datasource;

import lombok.Data;

import java.util.Date;

/**
 * @program: dataease
 * @description:
 * @author: wanziliang
 * @create: 2024-06-11 17:52
 **/
@Data
public class OpcUaData {

    private String value;
    private String status;
    private String serverTime;
    private String sourceTime;
    private String serverPicoseconds;
    private String sourcePicoseconds;
    private String currentTime;
    private String nodeId;

}
