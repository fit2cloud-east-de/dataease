package io.dataease.provider.datasource;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
import io.dataease.commons.utils.HttpClientConfig;
import io.dataease.commons.utils.HttpClientUtil;
import io.dataease.controller.request.datasource.ApiDefinition;
import io.dataease.controller.request.datasource.ApiDefinitionRequest;
import io.dataease.dto.dataset.DatasetTableFieldDTO;
import io.dataease.exception.DataEaseException;
import io.dataease.plugins.common.dto.chart.ChartCustomFilterItemDTO;
import io.dataease.plugins.common.dto.chart.ChartFieldCustomFilterDTO;
import io.dataease.plugins.common.dto.datasource.TableDesc;
import io.dataease.plugins.common.dto.datasource.TableField;
import io.dataease.plugins.common.request.chart.ChartExtFilterRequest;
import io.dataease.plugins.common.request.datasource.DatasourceRequest;
import io.dataease.plugins.datasource.provider.Provider;
import io.dataease.service.system.SystemParameterService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service("piApiProvider")
public class PiApiProvider extends Provider {

    private static String path = "['%s']";

    @Resource
    private SystemParameterService systemParameterService;

    @Override
    public List<String[]> getData(DatasourceRequest datasourceRequest) throws Exception {

        if(CollectionUtils.isNotEmpty(datasourceRequest.getPermissionFields())){
            return getFrequencyType();
        }

        JSONArray dataValues = executePiApiQuery(datasourceRequest.getDatasource().getConfiguration(), datasourceRequest.getInfo() , datasourceRequest.getChartExtFilterRequests());

        List<String[]> dataList = new ArrayList<>();

        List<ChartFieldCustomFilterDTO> filterDTOS = datasourceRequest.getFilterDTOS();

        Map <String , List<ChartCustomFilterItemDTO>> filters = new HashMap<>();

        filterDTOS.forEach(filterDTO->{
            List<ChartCustomFilterItemDTO> filterList = filterDTO.getFilter();
            String originName = filterDTO.getField().getOriginName();
            filters.put(originName , filterList);
        });


        for (int i = 0 ; i < dataValues.size() ; i++) {
            JSONObject dataValue = dataValues.getJSONObject(i);
            AtomicReference<Boolean> needContinue = new AtomicReference<>(false);
            if (CollectionUtils.isNotEmpty(filters.keySet())) {
                filters.forEach((k,v)->{
                    if (dataValue.containsKey(k)) {
                        v.stream().forEach(filterDTO -> {
                            //先只做等于
                            if (filterDTO.getTerm().equals("eq")) {
                                if (dataValue.getString(k).equals(filterDTO.getValue())) {
                                    needContinue.set(true);
                                }
                            }
                        });
                    }
                });
            }
            if (needContinue.get()) {
                continue;
            }
            dataList.add(new String[]{dataValue.getString("DataTime"),dataValue.getString("DataValue") });
        }

        Collections.sort(dataList, new Comparator<String[]>() {

                    @Override
                    public int compare(String[] o1, String[] o2) {

                        try {
                            Date date1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(o1[0]);
                            Date date2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(o2[0]);

                            if ( date1.before(date2) ){
                                return -1;
                            } else if (date1.after(date2) ) {
                                return 1;
                            } else {
                                return 0;
                            }

                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        return 0;
                    }
                });

        System.out.println(dataList.size());
        return dataList;
    }

    private List<String[]> getFrequencyType () {
        List<String[]> ls = new ArrayList<>();
        ls.add(new String[]{"1s"});
        ls.add(new String[]{"5s"});
        ls.add(new String[]{"10s"});
        ls.add(new String[]{"30s"});
        ls.add(new String[]{"1min"});
        ls.add(new String[]{"5min"});
        ls.add(new String[]{"10min"});
        ls.add(new String[]{"30min"});
        ls.add(new String[]{"1d"});

        return ls;
    }

    @Override
    public List<TableDesc> getTables(DatasourceRequest datasourceRequest) throws Exception {
        List<TableDesc> tableDescs = new ArrayList<>();
        return tableDescs;
    }

    @Override
    public List<TableField> fetchResultField(DatasourceRequest datasourceRequest) throws Exception {
        return null;
    }

    public JSONArray executePiApiQuery (String  configuration , String info , List<ChartExtFilterRequest> chartExtFilterRequests) {
        String oauthToken = getOauthToken(configuration);

        HttpClientConfig httpClientConfig = new HttpClientConfig();
        httpClientConfig.addHeader("Content-Type", "application/json");
        httpClientConfig.addHeader("Authorization", "Bearer "+oauthToken);

        JSONObject config = JSONObject.parseObject(configuration);
        JSONObject requestInfo = JSONObject.parseObject(info);

        String[] start_time_filter = {""};
        String[] end_time_filter = {""};
        String[] data_rate_filter = {""};

        if (CollectionUtils.isNotEmpty(chartExtFilterRequests)) {
            chartExtFilterRequests.forEach(chartExtFilterRequest -> {
                if (chartExtFilterRequest.getOperator().equals("between")) {
                    String format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format((Long.valueOf(chartExtFilterRequest.getValue().get(0))));
                    start_time_filter[0] = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format((Long.valueOf(chartExtFilterRequest.getValue().get(0))));
                    end_time_filter[0] = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format((Long.valueOf(chartExtFilterRequest.getValue().get(1))));
                }
                if (chartExtFilterRequest.getOperator().equals("eq")) {
                    String s = chartExtFilterRequest.getValue().get(0);
                    if (s.endsWith("s")) {
                        data_rate_filter[0] = s.replace("s","");
                    } else if (s.endsWith("min")) {
                        data_rate_filter[0] = (Integer.parseInt(s.replace("min",""))*60)+"";
                    } else if (s.endsWith("d")) {
                        data_rate_filter[0] = (Integer.parseInt(s.replace("d",""))*24*60*60)+"";
                    }
                }
                
            });
        }

        String url = config.getString("url");
        String point_name = requestInfo.getString("point_name");
        String data_rate = StringUtils.isEmpty(data_rate_filter[0])? requestInfo.getString("data_rate") : data_rate_filter[0];
        String start_time =  StringUtils.isEmpty(start_time_filter[0])? requestInfo.getString("start_time") : start_time_filter[0];
        String end_time = StringUtils.isEmpty(end_time_filter[0])? requestInfo.getString("end_time") : end_time_filter[0];

        JSONObject jsonObject = new JSONObject();
        JSONArray pointData = new JSONArray();
        JSONObject onepoint = new JSONObject();
        onepoint.put("PointCode" , point_name);
        onepoint.put("PointType" , "all");
        onepoint.put("DataRate" , data_rate);
        onepoint.put("StartTime" , start_time);
        onepoint.put("EndTime" , end_time);
        pointData.add(onepoint);
        jsonObject.put("PointData" , pointData);
        jsonObject.put("dpCounts" , 5);

        String response = HttpClientUtil.post(url+"/AFPI/History/GetRangePointsData", jsonObject.toJSONString(), httpClientConfig);

        JSONObject resp = JSONObject.parseObject(response);

        JSONArray resultData = resp.getJSONArray("ResultData");
        JSONArray dataValues = resultData.getJSONObject(0).getJSONArray("DataValue");

        return dataValues;
    }

    public Map<String, List> fetchResultAndField(String  configuration , String info) throws Exception {

        JSONArray dataValues = executePiApiQuery(configuration, info , null);

        List<String[]> dataList = new ArrayList<>();

        for (int i = 0 ; i < dataValues.size() ; i++) {
            JSONObject dataValue = dataValues.getJSONObject(i);
            dataList.add(new String[]{dataValue.getString("DataValue") , dataValue.getString("DataStatus"),
                    dataValue.getString("DataTime") , dataValue.getString("PointCode"),
                    dataValue.getString("ValueType")  });
        }



        Map<String, List> result = new HashMap<>();

        result.put("fieldList", getTableFields());
        result.put("dataList", dataList);
        return result;
    }

    public Map<String, List> fetchResultAndField(DatasourceRequest datasourceRequest) throws Exception {
        return fetchResultAndField(datasourceRequest.getDatasource().getConfiguration() ,datasourceRequest.getInfo());
    }


    public List<TableField> getTableFields(){
        List<TableField> fieldList = new ArrayList<>();
        fieldList.add(TableField.builder().fieldName("DataValue").remarks("DataValue").fieldType("3").build());
        fieldList.add(TableField.builder().fieldName("DataStatus").remarks("DataStatus").fieldType("0").build());
        fieldList.add(TableField.builder().fieldName("DataTime").remarks("DataTime").fieldType("1").build());
        fieldList.add(TableField.builder().fieldName("PointCode").remarks("PointCode").fieldType("0").build());
        fieldList.add(TableField.builder().fieldName("ValueType").remarks("ValueType").fieldType("0").build());
        return fieldList;
    }

    public List<TableField> getTableFields(DatasourceRequest datasourceRequest) throws Exception {
        return getTableFields();
    }

    public String checkStatus(DatasourceRequest datasourceRequest) throws Exception {
        String token = getOauthToken(datasourceRequest);
        return "Success";
    }


    private String getOauthToken (String  configuration) {

        HttpClientConfig httpClientConfig = new HttpClientConfig();
        httpClientConfig.addHeader("Content-Type", "application/x-www-form-urlencoded");

        JSONObject config = JSONObject.parseObject(configuration);

        String url = config.getString("url");
        String client_id = config.getString("client_id");
        String client_secret = config.getString("client_secret");

        Map<String, String> body = new HashMap<>();
        body.put("client_id" , client_id);
        body.put("client_secret" , client_secret);
        body.put("grant_type" , "client_credentials");


        String response = HttpClientUtil.post(url+"/oauth2/token", body, httpClientConfig);

        JSONObject resp = JSONObject.parseObject(response);

        if (!resp.containsKey("access_token")) {
            DataEaseException.throwException(response);
        }

        return resp.getString("access_token");

    }


    private String getOauthToken (DatasourceRequest datasourceRequest) {
        return getOauthToken(datasourceRequest.getDatasource().getConfiguration());
    }

    static public String execHttpRequest(ApiDefinition apiDefinition, int socketTimeout) throws Exception {
        String response = "";
        HttpClientConfig httpClientConfig = new HttpClientConfig();
        httpClientConfig.setSocketTimeout(socketTimeout * 1000);
        ApiDefinitionRequest apiDefinitionRequest = apiDefinition.getRequest();
        for (Map header : apiDefinitionRequest.getHeaders()) {
            if (header.get("name") != null && StringUtils.isNotEmpty(header.get("name").toString()) && header.get("value") != null && StringUtils.isNotEmpty(header.get("value").toString())) {
                httpClientConfig.addHeader(header.get("name").toString(), header.get("value").toString());
            }
        }
        if (apiDefinitionRequest.getAuthManager() != null
                && StringUtils.isNotBlank(apiDefinitionRequest.getAuthManager().getUsername())
                && StringUtils.isNotBlank(apiDefinitionRequest.getAuthManager().getPassword())
                && apiDefinitionRequest.getAuthManager().getVerification().equals("Basic Auth")) {
            String authValue = "Basic " + Base64.getUrlEncoder().encodeToString((apiDefinitionRequest.getAuthManager().getUsername()
                    + ":" + apiDefinitionRequest.getAuthManager().getPassword()).getBytes());
            httpClientConfig.addHeader("Authorization", authValue);
        }

        switch (apiDefinition.getMethod()) {
            case "GET":
                List<String>  params = new ArrayList<>();
                for (Map<String, String> argument : apiDefinition.getRequest().getArguments()) {
                    if(StringUtils.isNotEmpty(argument.get("name")) && StringUtils.isNotEmpty(argument.get("value"))){
                        params.add(argument.get("name") + "=" + URLEncoder.encode(argument.get("value")));
                    }
                }
                if(CollectionUtils.isNotEmpty(params)){
                    apiDefinition.setUrl(apiDefinition.getUrl() + "?" + StringUtils.join(params, "&"));
                }
                response = HttpClientUtil.get(apiDefinition.getUrl().trim(), httpClientConfig);
                break;
            case "POST":
                if (apiDefinitionRequest.getBody().get("type") == null) {
                    throw new Exception("请求类型不能为空");
                }
                String type = apiDefinitionRequest.getBody().get("type").toString();
                if (StringUtils.equalsAny(type, "JSON", "XML", "Raw")) {
                    String raw = null;
                    if (apiDefinitionRequest.getBody().get("raw") != null) {
                        raw = apiDefinitionRequest.getBody().get("raw").toString();
                        response = HttpClientUtil.post(apiDefinition.getUrl(), raw, httpClientConfig);
                    }
                }
                if (StringUtils.equalsAny(type, "Form_Data", "WWW_FORM")) {
                    if (apiDefinitionRequest.getBody().get("kvs") != null) {
                        Map<String, String> body = new HashMap<>();
                        JSONObject bodyObj = JSONObject.parseObject(apiDefinitionRequest.getBody().toString());
                        JSONArray kvsArr = bodyObj.getJSONArray("kvs");
                        for (int i = 0; i < kvsArr.size(); i++) {
                            JSONObject kv = kvsArr.getJSONObject(i);
                            if (kv.containsKey("name")) {
                                body.put(kv.getString("name"), kv.getString("value"));
                            }
                        }
                        response = HttpClientUtil.post(apiDefinition.getUrl(), body, httpClientConfig);
                    }
                }
                break;
            default:
                break;
        }
        return response;
    }


    static public ApiDefinition checkApiDefinition(ApiDefinition apiDefinition, String response) throws Exception {
        if (StringUtils.isEmpty(response)) {
            throw new Exception("该请求返回数据为空");
        }
        List<JSONObject> fields = new ArrayList<>();
        if (apiDefinition.isUseJsonPath() && !apiDefinition.isShowApiStructure()) {
            List<LinkedHashMap> currentData = new ArrayList<>();
            Object object = JsonPath.read(response, apiDefinition.getJsonPath());
            if (object instanceof List) {
                currentData = (List<LinkedHashMap>) object;
            } else {
                currentData.add((LinkedHashMap) object);
            }
            int i = 0;
            for (LinkedHashMap data : currentData) {
                if (i >= apiDefinition.getPreviewNum()) {
                    break;
                }
                if (i == 0) {
                    for (Object o : data.keySet()) {
                        JSONObject field = new JSONObject();
                        field.put("originName", o.toString());
                        field.put("name", o.toString());
                        field.put("type", "STRING");
                        field.put("checked", true);
                        field.put("size", 65535);
                        field.put("deExtractType", 0);
                        field.put("deType", 0);
                        field.put("extField", 0);
                        fields.add(field);
                    }
                }
                for (JSONObject field : fields) {
                    JSONArray array = field.getJSONArray("value");
                    if (array != null) {
                        array.add(Optional.ofNullable(data.get(field.getString("originName"))).orElse("").toString().replaceAll("\n", " ").replaceAll("\r", " "));
                    } else {
                        array = new JSONArray();
                        array.add(Optional.ofNullable(data.get(field.getString("originName"))).orElse("").toString().replaceAll("\n", " ").replaceAll("\r", " "));
                    }
                    field.put("value", array);
                }
                i++;
            }

            apiDefinition.setJsonFields(fields);
            return apiDefinition;
        } else {

            String rootPath;
            if (response.startsWith("[")) {
                rootPath = "$[*]";
                JSONArray jsonArray = JSONObject.parseArray(response);
                for (Object o : jsonArray) {
                    handleStr(apiDefinition, o.toString(), fields, rootPath);
                }
            } else {
                rootPath = "$";
                handleStr(apiDefinition, response, fields, rootPath);
            }
            for (JSONObject field : fields) {
                if (field.containsKey("children") && CollectionUtils.isNotEmpty(field.getJSONArray("children"))) {
                    field.put("disabled", false);
                }
                if (field.containsKey("children") && CollectionUtils.isEmpty(field.getJSONArray("children"))) {
                    field.put("disabled", true);
                }
            }
            apiDefinition.setJsonFields(fields);
            return apiDefinition;
        }
    }


    static private void handleStr(ApiDefinition apiDefinition, String jsonStr, List<JSONObject> fields, String rootPath) {
        if (jsonStr.startsWith("[")) {
            JSONArray jsonArray = JSONObject.parseArray(jsonStr);
            for (Object o : jsonArray) {
                handleStr(apiDefinition, o.toString(), fields, rootPath);
            }
        } else {
            JSONObject jsonObject = JSONObject.parseObject(jsonStr);
            for (String s : jsonObject.keySet()) {
                String value = jsonObject.getString(s);
                if (StringUtils.isNotEmpty(value) && value.startsWith("[")) {
                    JSONObject o = new JSONObject();
                    try {
                        JSONArray jsonArray = jsonObject.getJSONArray(s);
                        List<JSONObject> childrenField = new ArrayList<>();
                        for (Object object : jsonArray) {
                            handleStr(apiDefinition, JSON.toJSONString(object, SerializerFeature.WriteMapNullValue), childrenField, rootPath + "." + s + "[*]");
                        }
                        o.put("children", childrenField);
                        o.put("childrenDataType", "LIST");

                    } catch (Exception e) {
                        JSONArray array = new JSONArray();
                        array.add(StringUtils.isNotEmpty(jsonObject.getString(s)) ? jsonObject.getString(s) : "");
                        o.put("value", array);
                    }
                    o.put("jsonPath", rootPath + "." + String.format(path, s));
                    setProperty(apiDefinition, o, s);
                    if (!hasItem(apiDefinition, fields, o)) {
                        fields.add(o);
                    }
                } else if (StringUtils.isNotEmpty(value) && value.startsWith("{")) {
                    try {
                        JSONObject.parseObject(jsonStr);
                        List<JSONObject> children = new ArrayList<>();
                        handleStr(apiDefinition, jsonObject.getString(s), children, rootPath + "." + String.format(path, s));
                        JSONObject o = new JSONObject();
                        o.put("children", children);
                        o.put("childrenDataType", "OBJECT");
                        o.put("jsonPath", rootPath + "." + s);
                        setProperty(apiDefinition, o, s);
                        if (!hasItem(apiDefinition, fields, o)) {
                            fields.add(o);
                        }
                    }catch (Exception e){
                        JSONObject o = new JSONObject();
                        o.put("jsonPath", rootPath + "." + String.format(path, s));
                        setProperty(apiDefinition, o, s);
                        JSONArray array = new JSONArray();
                        array.add(StringUtils.isNotEmpty(jsonObject.getString(s)) ? jsonObject.getString(s) : "");
                        o.put("value", array);
                        if (!hasItem(apiDefinition, fields, o)) {
                            fields.add(o);
                        }
                    }
                } else {
                    JSONObject o = new JSONObject();
                    o.put("jsonPath", rootPath + "." + String.format(path, s));
                    setProperty(apiDefinition, o, s);
                    JSONArray array = new JSONArray();
                    array.add(StringUtils.isNotEmpty(jsonObject.getString(s)) ? jsonObject.getString(s) : "");
                    o.put("value", array);
                    if (!hasItem(apiDefinition, fields, o)) {
                        fields.add(o);
                    }
                }

            }
        }
    }

    static private void setProperty(ApiDefinition apiDefinition, JSONObject o, String s) {
        o.put("originName", s);
        o.put("name", s);
        o.put("type", "STRING");
        o.put("checked", false);
        o.put("size", 65535);
        o.put("deExtractType", 0);
        o.put("deType", 0);
        o.put("extField", 0);
        o.put("checked", false);
        if (!apiDefinition.isUseJsonPath()) {
            for (DatasetTableFieldDTO fieldDTO : apiDefinition.getFields()) {
                if (StringUtils.isNotEmpty(o.getString("jsonPath")) && StringUtils.isNotEmpty(fieldDTO.getJsonPath()) && fieldDTO.getJsonPath().equals(o.getString("jsonPath"))) {
                    o.put("checked", true);
                    o.put("deExtractType", fieldDTO.getDeExtractType());
                    o.put("name", fieldDTO.getName());
                }
            }
        }
    }

    static private boolean hasItem(ApiDefinition apiDefinition, List<JSONObject> fields, JSONObject item) {
        boolean has = false;
        for (JSONObject field : fields) {
            if (field.getString("jsonPath").equals(item.getString("jsonPath"))) {
                has = true;
                mergeField(field, item);
                mergeValue(field, apiDefinition, item);
                break;
            }
        }

        return has;
    }


    static void mergeField(JSONObject field, JSONObject item) {
        if (item.getJSONArray("children") != null) {
            JSONArray itemChildren = item.getJSONArray("children");
            JSONArray fieldChildren = field.getJSONArray("children");
            if (fieldChildren == null) {
                fieldChildren = new JSONArray();
            }
            for (Object itemChild : itemChildren) {
                boolean hasKey = false;
                JSONObject itemChildObject = JSONObject.parseObject(itemChild.toString());
                for (Object fieldChild : fieldChildren) {
                    JSONObject fieldChildObject = JSONObject.parseObject(fieldChild.toString());
                    if (itemChildObject.getString("jsonPath").equals(fieldChildObject.getString("jsonPath"))) {
                        mergeField(fieldChildObject, itemChildObject);
                        hasKey = true;
                    }
                }
                if (!hasKey) {
                    fieldChildren.add(itemChild);
                }
            }
        }
    }

    static void mergeValue(JSONObject field, ApiDefinition apiDefinition, JSONObject item) {
        JSONArray array = field.getJSONArray("value");
        if (array != null && item.getString("value") != null && array.size() < apiDefinition.getPreviewNum()) {
            array.add(item.getJSONArray("value").get(0).toString());
            field.put("value", array);
        }
        if (CollectionUtils.isNotEmpty(field.getJSONArray("children")) && CollectionUtils.isNotEmpty(item.getJSONArray("children"))) {
            JSONArray fieldChildren = field.getJSONArray("children");
            JSONArray itemChildren = item.getJSONArray("children");

            JSONArray fieldArrayChildren = new JSONArray();
            for (Object fieldChild : fieldChildren) {
                JSONObject jsonObject = JSONObject.parseObject(fieldChild.toString());
                JSONObject find = null;
                for (Object itemChild : itemChildren) {
                    JSONObject itemObject = JSONObject.parseObject(itemChild.toString());
                    if (jsonObject.getString("jsonPath").equals(itemObject.getString("jsonPath"))) {
                        find = itemObject;
                    }
                }
                if (find != null) {
                    mergeValue(jsonObject, apiDefinition, find);
                }
                fieldArrayChildren.add(jsonObject);
            }
            field.put("children", fieldArrayChildren);
        }
    }

    private List<String[]> fetchResult(String result, ApiDefinition apiDefinition) {
        List<String[]> dataList = new LinkedList<>();
        if(apiDefinition.isUseJsonPath()){
            List<LinkedHashMap> currentData = new ArrayList<>();
            Object object = JsonPath.read(result, apiDefinition.getJsonPath());
            if (object instanceof List) {
                currentData = (List<LinkedHashMap>) object;
            } else {
                currentData.add((LinkedHashMap) object);
            }
            for (LinkedHashMap data : currentData) {
                String[] row = new String[apiDefinition.getFields().size()];
                int i = 0;
                for (DatasetTableFieldDTO field : apiDefinition.getFields()) {
                    row[i] = Optional.ofNullable(data.get(field.getOriginName())).orElse("").toString().replaceAll("\n", " ").replaceAll("\r", " ");
                    i++;
                }
                dataList.add(row);
            }
        }else {
            if (StringUtils.isNotEmpty(apiDefinition.getDataPath()) && CollectionUtils.isEmpty(apiDefinition.getJsonFields())) {
                List<LinkedHashMap> currentData = new ArrayList<>();
                Object object = JsonPath.read(result, apiDefinition.getDataPath());
                if (object instanceof List) {
                    currentData = (List<LinkedHashMap>) object;
                } else {
                    currentData.add((LinkedHashMap) object);
                }
                for (LinkedHashMap data : currentData) {
                    String[] row = new String[apiDefinition.getFields().size()];
                    int i = 0;
                    for (DatasetTableFieldDTO field : apiDefinition.getFields()) {
                        row[i] = Optional.ofNullable(data.get(field.getOriginName())).orElse("").toString().replaceAll("\n", " ").replaceAll("\r", " ");
                        i++;
                    }
                    dataList.add(row);
                }
            } else {
                List<String> jsonPaths = apiDefinition.getFields().stream().map(DatasetTableFieldDTO::getJsonPath).collect(Collectors.toList());
                Long maxLength = 0l;
                List<List<String>> columnDataList = new ArrayList<>();
                for (int i = 0; i < jsonPaths.size(); i++) {
                    List<String> data = new ArrayList<>();
                    Object object = JsonPath.read(result, jsonPaths.get(i));
                    if (object instanceof List && jsonPaths.get(i).contains("[*]")) {
                        data = (List<String>) object;
                    } else {
                        if (object != null) {
                            data.add(object.toString());
                        }
                    }
                    maxLength = maxLength > data.size() ? maxLength : data.size();
                    columnDataList.add(data);
                }
                for (int i = 0; i < maxLength; i++) {
                    String[] row = new String[apiDefinition.getFields().size()];
                    dataList.add(row);
                }
                for (int i = 0; i < columnDataList.size(); i++) {
                    for (int j = 0; j < columnDataList.get(i).size(); j++) {
                        dataList.get(j)[i] = Optional.ofNullable(String.valueOf(columnDataList.get(i).get(j))).orElse("").replaceAll("\n", " ").replaceAll("\r", " ");
                    }
                }
            }
        }
        return dataList;
    }


    private ApiDefinition checkApiDefinition(DatasourceRequest datasourceRequest) throws Exception {
        List<ApiDefinition> apiDefinitionList = new ArrayList<>();
        List<ApiDefinition> apiDefinitionListTemp = new Gson().fromJson(datasourceRequest.getDatasource().getConfiguration(), new TypeToken<List<ApiDefinition>>() {
        }.getType());
        if (CollectionUtils.isNotEmpty(apiDefinitionListTemp)) {
            for (ApiDefinition apiDefinition : apiDefinitionListTemp) {
                if (apiDefinition.getName().equalsIgnoreCase(datasourceRequest.getTable())) {
                    apiDefinitionList.add(apiDefinition);
                }

            }
        }
        if (CollectionUtils.isEmpty(apiDefinitionList)) {
            throw new Exception("未找到API数据表");
        }
        if (apiDefinitionList.size() > 1) {
            throw new Exception("存在重名的API数据表");
        }
        for (ApiDefinition apiDefinition : apiDefinitionList) {
            if (apiDefinition.getName().equalsIgnoreCase(datasourceRequest.getTable())) {
                return apiDefinition;
            }
        }
        throw new Exception("未找到API数据表");
    }

}
