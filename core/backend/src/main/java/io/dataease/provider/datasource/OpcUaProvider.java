package io.dataease.provider.datasource;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import io.dataease.controller.request.datasource.ApiDefinition;
import io.dataease.controller.request.datasource.OpcUaData;
import io.dataease.controller.request.datasource.OpcUaDefinitionRequest;
import io.dataease.dto.dataset.DatasetTableFieldDTO;
import io.dataease.plugins.common.dto.datasource.TableDesc;
import io.dataease.plugins.common.dto.datasource.TableField;
import io.dataease.plugins.common.request.datasource.DatasourceRequest;
import io.dataease.plugins.datasource.entity.Status;
import io.dataease.plugins.datasource.provider.Provider;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.UaClient;
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
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * @program: dataease
 * @description:
 * @author: wanziliang
 * @create: 2024-06-11 12:01
 **/
@Service("opcUaProvider")
public class OpcUaProvider extends Provider {

    private static Semaphore semaphore = new Semaphore(5);

    @Override
    public List<String[]> getData(DatasourceRequest datasourceRequest) throws Exception {
        return null;
    }

    @Override
    public List<TableDesc> getTables(DatasourceRequest datasourceRequest) throws Exception {
        return null;
    }

    @Override
    public String checkStatus(DatasourceRequest datasourceRequest) throws Exception {
        return null;
    }

    @Override
    public Status checkDsStatus(DatasourceRequest datasourceRequest) throws Exception {
        Status status = new Status();
        Gson gson = new Gson();
        OpcUaDefinitionRequest opcUaDefinitionRequest = gson.fromJson(datasourceRequest.getDatasource().getConfiguration(), OpcUaDefinitionRequest.class);
        OpcUaClient client = null;
        try {
            client = createClient(opcUaDefinitionRequest);
            client.connect();
            status.setStatus("Success");
        } catch ( Exception e ) {
            status.setStatus("Failed");
        } finally {
            if (null != client) {
                client.disconnect();
            }
        }
        return status;
    }


    public OpcUaClient createClient(OpcUaDefinitionRequest opcUaDefinitionRequest) throws UaException, ExecutionException, InterruptedException {

        String endPoint = opcUaDefinitionRequest.getEndpoint();

        List<EndpointDescription> endpoints = DiscoveryClient.getEndpoints(endPoint).get();

        Optional<EndpointDescription> first = endpoints.stream().filter(e -> e.getSecurityPolicyUri().equals(SecurityPolicy.None.getUri())).findFirst();

        EndpointDescription endpointDescription = null;

        if (first.isPresent()) {
            endpointDescription = first.get();
        } else {
            throw new RuntimeException("can not find endpoint");
        }

        String host = endPoint.substring(endPoint.indexOf("//")+2 , endPoint.lastIndexOf(":"));
        String port = endPoint.substring( endPoint.lastIndexOf(":")+1);
        port = port.substring(0, port.indexOf("/"));

        EndpointDescription configPoint =
                EndpointUtil.updateUrl(endpointDescription,host, Integer.parseInt(port));

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


    @Override
    public List<TableField> fetchResultField(DatasourceRequest datasourceRequest) throws Exception {
        return null;
    }

    @Override
    public Map<String, List> fetchResultAndField(DatasourceRequest datasourceRequest) throws Exception {

        Map<String, List> result = new HashMap<>();

        Gson gson = new Gson();
        OpcUaDefinitionRequest opcUaDefinitionRequest = gson.fromJson(datasourceRequest.getDatasource().getConfiguration(), OpcUaDefinitionRequest.class);

        OpcUaClient client = null;

        try {

            client = createClient(opcUaDefinitionRequest);

            List<TableField> tableFields = getTableFields(OpcUaData.class);

            List<String> tables = datasourceRequest.getTables();

            List<String[]> dataList = new ArrayList<>();

            List<OpcUaData> opcUaDataList = readNodes(client, tables);
            opcUaDataList.forEach(opcUaData -> {
                String[] str = fetchResult(opcUaData, tableFields);
                dataList.add(str);
            });

            result.put("fieldList", tableFields);
            result.put("dataList", dataList);

        } catch ( Exception e ) {
            e.printStackTrace();
        }  finally {
            if (null != client) {
                client.disconnect();
            }
        }
        return result;
    }


    private List<OpcUaData> readNodes (OpcUaClient client , List<String> nodeIds)  throws Exception {

        //获取一把锁
        semaphore.acquire();

        List<OpcUaData> opcUaDataList = new ArrayList<>();

        client.connect().get();

        List<DataValue> dataValueList = client.readValues(
                0.0,
                TimestampsToReturn.Both,
                nodeIds.stream().map(NodeId::parse).collect(Collectors.toList())
        ).get();

        for (int i = 0; i < dataValueList.size(); i++) {
            opcUaDataList.add(transformOpcuaData(dataValueList.get(i), nodeIds.get(i))) ;
        }

        //释放一把锁
        semaphore.release();

        return opcUaDataList;
    }


    private OpcUaData transformOpcuaData (DataValue dataValue , String  nodeId) {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        OpcUaData opcUaData = new OpcUaData();

        Variant value = dataValue.getValue();

        opcUaData.setValue(value.getValue().toString());
        opcUaData.setNodeId(nodeId);
        opcUaData.setServerTime( dataValue.getServerTime() == null ? null : simpleDateFormat.format(dataValue.getServerTime().getJavaDate()));
        opcUaData.setSourceTime( dataValue.getSourceTime() == null ? null : simpleDateFormat.format(dataValue.getSourceTime().getJavaDate()));
        opcUaData.setCurrentTime(simpleDateFormat.format(new Date()));
        opcUaData.setServerPicoseconds(dataValue.getServerPicoseconds()==null?null:dataValue.getServerPicoseconds().toString());
        opcUaData.setSourcePicoseconds(dataValue.getSourcePicoseconds()==null?null: dataValue.getSourcePicoseconds().toString());

        opcUaData.setStatus(dataValue.getStatusCode().isGood() ? "Good" : dataValue.getStatusCode().isBad() ? "Bad" : dataValue.getStatusCode().isUncertain() ? "uncertain" : "Unknown" );

        return opcUaData;
    }

    private OpcUaData readNode(OpcUaClient client , String nodeId) throws Exception {

        client.connect().get();

        DataValue dataValue = client.readValue(
                0.0,
                TimestampsToReturn.Both,
                NodeId.parse(nodeId)
        ).get();

        OpcUaData opcUaData = transformOpcuaData(dataValue, nodeId);

        client.disconnect().get();

        return opcUaData;
    }

    public List<TableField> getTableFields(DatasourceRequest datasourceRequest) throws Exception {
        return getTableFields(OpcUaData.class);
    }

    private List<TableField> getTableFields(Class claz)   {
        List<TableField> tableFields = new ArrayList<>();

        Field[] declaredFields = claz.getDeclaredFields();
        for (Field field : declaredFields) {
            TableField tableField = new TableField();
            field.setAccessible(true);

            tableField.setFieldName(field.getName());
            tableField.setRemarks(field.getName());
            tableField.setField(field);
            tableField.setFieldType("VARCHAR");
            tableField.setFieldSize(255);

            tableFields.add(tableField);
        }
        return tableFields;
    }

    private String[] fetchResult(OpcUaData opcUaData , List<TableField> fieldList) {
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
