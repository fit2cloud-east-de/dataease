package io.dataease.datasource.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.dataease.api.ds.vo.OpcUaData;
import io.dataease.api.ds.vo.OpcUaDefinitionRequest;
import io.dataease.exception.DEException;
import io.dataease.extensions.datasource.dto.DatasetTableDTO;
import io.dataease.extensions.datasource.dto.DatasourceDTO;
import io.dataease.extensions.datasource.dto.DatasourceRequest;
import io.dataease.extensions.datasource.dto.TableField;
import io.dataease.utils.CommonBeanFactory;
import io.dataease.utils.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.util.EndpointUtil;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class OPCUAProvider {

    public static String checkStatus(DatasourceRequest datasourceRequest) throws Exception {
        OpcUaDefinitionRequest opcUaDefinitionRequest = JsonUtil.parseObject(datasourceRequest.getDatasource().getConfiguration(), OpcUaDefinitionRequest.class);
        OpcUaClient client = null;
        try {
            client = createClient(opcUaDefinitionRequest);
            client.connect();
            return "Success";
        } catch ( Exception e ) {
            return "Error";
        } finally {
            if (null != client) {
                client.disconnect();
            }
        }
    }

    public static List<OpcUaData> readNodes (DatasourceRequest datasourceRequest) throws Exception {

        List<OpcUaData> opcUaDataList = new ArrayList<OpcUaData>();

        OpcUaDefinitionRequest opcUaDefinitionRequest = JsonUtil.parseObject(datasourceRequest.getDatasource().getConfiguration(), OpcUaDefinitionRequest.class);

        OpcUaClient client = null;
        try {
            client = createClient(opcUaDefinitionRequest);
            client.connect();

            List<String> nodeList = opcUaDefinitionRequest.getNodeList();
            for (String node : nodeList) {
                opcUaDataList.add(readNode(client ,node ));
            }
        } catch ( Exception e) {
            e.printStackTrace();
           throw new RuntimeException(e.getMessage());
        } finally {
            if (null != client) {
                client.disconnect();
            }
        }

        return opcUaDataList;
    }


    public static OpcUaClient createClient(OpcUaDefinitionRequest opcUaDefinitionRequest) throws UaException, ExecutionException, InterruptedException {

        String endPoint = opcUaDefinitionRequest.getEndpoint();

        List<EndpointDescription> endpoints = DiscoveryClient.getEndpoints(endPoint).get();

        Optional<EndpointDescription> first = endpoints.stream().filter(e -> e.getSecurityPolicyUri().equals(SecurityPolicy.None.getUri())).findFirst();

        EndpointDescription endpointDescription = null;

        if (first.isPresent()) {
            endpointDescription = first.get();
        } else {
            throw new RuntimeException("can not find endpoint");
        }

        EndpointDescription configPoint = EndpointUtil.updateUrl(endpointDescription, endPoint.substring(endPoint.indexOf("//")+2 , endPoint.lastIndexOf(":")), Integer.parseInt(endPoint.substring( endPoint.lastIndexOf(":")+1)));

        OpcUaClientConfigBuilder cfg = new OpcUaClientConfigBuilder();
        cfg.setEndpoint(configPoint);
        cfg.setApplicationName(LocalizedText.english("eclipse milo opc-ua client"));
        cfg.setApplicationUri("urn:eclipse:milo:examples:client");
        cfg.setRequestTimeout(UInteger.valueOf(5000));

        if (StringUtils.isEmpty(opcUaDefinitionRequest.getOpcUaUsername())) {
            cfg.setIdentityProvider(new AnonymousProvider());
        } else {
            cfg.setIdentityProvider(new UsernameProvider(opcUaDefinitionRequest.getOpcUaUsername(), opcUaDefinitionRequest.getOpcUaPassword()));
        }

        return OpcUaClient.create(cfg.build());

    }


    public static OpcUaData readNode(OpcUaClient client, String nodeId) throws Exception {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        client.connect().get();

        DataValue dataValue = client.readValue(
                0.0,
                TimestampsToReturn.Both,
                NodeId.parse(nodeId)
        ).get();

        OpcUaData opcUaData = new OpcUaData();

        Variant value = dataValue.getValue();

        if (null == value) {
            return opcUaData;
        }

        opcUaData.setValue(null == value.getValue() ? null : value.getValue().toString());

        opcUaData.setNodeId(nodeId);

        opcUaData.setServerTime( dataValue.getServerTime() == null ? null : simpleDateFormat.format(dataValue.getServerTime().getJavaDate()));
        opcUaData.setSourceTime( dataValue.getSourceTime() == null ? null : simpleDateFormat.format(dataValue.getSourceTime().getJavaDate()));
        opcUaData.setCurrentTime(simpleDateFormat.format(new Date()));

        opcUaData.setServerPicoseconds(dataValue.getServerPicoseconds()==null?null:dataValue.getServerPicoseconds().toString());
        opcUaData.setSourcePicoseconds(dataValue.getSourcePicoseconds()==null?null: dataValue.getSourcePicoseconds().toString());

        if ( null != dataValue.getStatusCode()) {
            opcUaData.setStatus(dataValue.getStatusCode().isGood() ? "Good" : dataValue.getStatusCode().isBad() ? "Bad" : dataValue.getStatusCode().isUncertain() ? "uncertain" : "Unknown" );
        }

        client.disconnect().get();

        return opcUaData;
    }


    public static List<DatasetTableDTO> getTables (DatasourceRequest datasourceRequest ) {
        OpcUaDefinitionRequest opcUaDefinitionRequest = JsonUtil.parseObject(datasourceRequest.getDatasource().getConfiguration(), OpcUaDefinitionRequest.class);
        List<DatasetTableDTO> datasetTableDTOS = new ArrayList<>();
        //直连
        if ("direct".equals(opcUaDefinitionRequest.getConnectionType())) {
            for (String nodeId: opcUaDefinitionRequest.getNodeList()) {
                DatasetTableDTO datasetTableDTO = new DatasetTableDTO();
                datasetTableDTO.setTableName(nodeId);
                datasetTableDTO.setName(nodeId);
                datasetTableDTO.setDatasourceId(datasourceRequest.getDatasource().getId());
                datasetTableDTOS.add(datasetTableDTO);
            }
        } else {
        // todo 定时同步



        }
        return datasetTableDTOS;
    }


    public static List<TableField> getTableFields() {
        return getTableFields(OpcUaData.class , Boolean.FALSE);
    }

    private static List<TableField> getTableFields(Class claz , Boolean setField)   {
        List<TableField> tableFields = new ArrayList<>();

        Field[] declaredFields = claz.getDeclaredFields();
        for (Field field : declaredFields) {
            TableField tableField = new TableField();
            field.setAccessible(true);

            tableField.setName(field.getName());
            tableField.setOriginName(field.getName());

            if (field.getName().equals("value")) {
                tableField.setDeType(3);
                tableField.setDeExtractType(3);
                tableField.setType("DECIMAL");
            } else if (field.getName().equals("serverTime") || field.getName().equals("sourceTime") || field.getName().equals("currentTime")) {
                tableField.setDeType(1);
                tableField.setDeExtractType(1);
                tableField.setType("DATETIME");
            } else {
                tableField.setDeType(0);
                tableField.setDeExtractType(0);
                tableField.setType("VARCHAR");
            }

            if (setField) {
                tableField.setField(field);
            }

            tableFields.add(tableField);
        }
        return tableFields;
    }

    public static Map<String, Object> fetchResultAndField(DatasourceRequest datasourceRequest) {

        Map<String, Object> result = new HashMap<>();

        OpcUaDefinitionRequest opcUaDefinitionRequest = JsonUtil.parseObject(datasourceRequest.getDatasource().getConfiguration(), OpcUaDefinitionRequest.class);

        OpcUaClient client = null;

        try {

            client = createClient(opcUaDefinitionRequest);

            List<TableField> tableFields = getTableFields(OpcUaData.class , Boolean.TRUE);

            List<String> tables = Collections.singletonList(datasourceRequest.getTable());

            List<String[]> dataList = new ArrayList<>();

            for (String table: tables  ) {
                OpcUaData opcUaData = readNode(client, table);
                String[] str = fetchResult(opcUaData, tableFields);
                dataList.add(str);
            }

            result.put("fields", tableFields);
            result.put("data", dataList);

        } catch ( Exception e ) {
            e.printStackTrace();
        }  finally {
            if (null != client) {
                client.disconnect();
            }
        }
        return result;
    }

    private static String[] fetchResult(OpcUaData opcUaData , List<TableField> fieldList) {
        String[] str = new String[fieldList.size()];
        fieldList.stream().forEach(tableField -> {
            try {
                str[fieldList.indexOf(tableField)] = tableField.getField().get(opcUaData) == null ? null : tableField.getField().get(opcUaData).toString();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
        return str;
    }

}
