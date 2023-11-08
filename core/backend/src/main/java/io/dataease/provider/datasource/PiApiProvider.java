package io.dataease.provider.datasource;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.dataease.commons.utils.HttpClientConfig;
import io.dataease.commons.utils.HttpClientUtil;
import io.dataease.controller.request.datasource.ApiDefinition;
import io.dataease.controller.request.datasource.ApiDefinitionRequest;
import io.dataease.exception.DataEaseException;
import io.dataease.plugins.common.dto.chart.ChartCustomFilterItemDTO;
import io.dataease.plugins.common.dto.chart.ChartFieldCustomFilterDTO;
import io.dataease.plugins.common.dto.chart.ChartViewFieldDTO;
import io.dataease.plugins.common.dto.datasource.TableDesc;
import io.dataease.plugins.common.dto.datasource.TableField;
import io.dataease.plugins.common.request.chart.ChartExtFilterRequest;
import io.dataease.plugins.common.request.datasource.DatasourceRequest;
import io.dataease.plugins.datasource.provider.Provider;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Service("piApiProvider")
public class PiApiProvider extends Provider {

    private static String path = "['%s']";

    @Resource
    private JdbcProvider jdbcProvider;


    @Override
    public List<String> getSchema(DatasourceRequest datasourceRequest) throws Exception{
        datasourceRequest.getDatasource().setType("sqlServer");
        List<String> schema = jdbcProvider.getSchema(datasourceRequest);
        datasourceRequest.getDatasource().setType("pi_api");
        return schema;
    }


    @Override
    public List<String[]> getData(DatasourceRequest datasourceRequest) throws Exception {

        
        //过滤器特殊处理
        if(CollectionUtils.isNotEmpty(datasourceRequest.getPermissionFields())){
            return getFrequencyType();
        }

        Map<String, List> piApiStringListMap = fetchResultAndField(datasourceRequest.getDatasource().getConfiguration(), datasourceRequest.getInfo() , datasourceRequest.getChartExtFilterRequests());
        Map<String, List> sqlServerStringListMap = fetchSqlServerResultAndField(datasourceRequest);

        Map<String, List> mergeMap = mergePiAndSqlServerDataAndField(piApiStringListMap, sqlServerStringListMap);

        List <TableField> fieldList = mergeMap.get("fieldList");
        List <String []> dataList = mergeMap.get("dataList");

        //视图过滤器
        List<ChartFieldCustomFilterDTO> filterDTOS = datasourceRequest.getFilterDTOS();
        Map <String , List<ChartCustomFilterItemDTO>> filters = new HashMap<>();

        filterDTOS.forEach(filterDTO->{
            List<ChartCustomFilterItemDTO> filterList = filterDTO.getFilter();
            String originName = filterDTO.getField().getOriginName();
            filters.put(originName , filterList);
        });

        AtomicReference<Boolean> firstFilter = new AtomicReference<>(Boolean.TRUE);
        List <String []> dataListAfterFilter = new ArrayList<>();
        dataListAfterFilter.addAll(dataList);

        filters.forEach((k,v)->{
            TableField tableField = fieldList.stream().filter(field -> field.getFieldName().equals(k)).findFirst().get();
            int i = fieldList.indexOf(tableField);
            v.forEach(filter -> {
                List <String []> middleList =  new ArrayList<>();
                List <String []> middleAfterFilterList =  new ArrayList<>();
                middleList.addAll(dataListAfterFilter);
                if (filter.getTerm().equals("eq")) {
                    if (!firstFilter.get()) {
                        middleList.forEach(str -> {
                            if (str[i].equals(filter.getValue())){
                                middleAfterFilterList.add(str);
                            }
                        });
                    } else {
                        middleList.forEach(str -> {
                            if (str[i].equals(filter.getValue())){
                                middleAfterFilterList.add(str);
                            }
                        });
                    }
                }
                dataListAfterFilter.clear();
                dataListAfterFilter.addAll(middleAfterFilterList);
                firstFilter.set(Boolean.FALSE);
            });
        });

        //按时间从小到大排序
        Collections.sort(dataListAfterFilter, new Comparator<String[]>() {
                    @Override
                    public int compare(String[] o1, String[] o2) {

                        try {
                            Date date1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(o1[2]);
                            Date date2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(o2[2]);

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

        // 过滤需要的字段，目前只考虑 x 、y 轴

        List<ChartViewFieldDTO> xAxis = datasourceRequest.getXAxis();
        List<ChartViewFieldDTO> yAxis = datasourceRequest.getYAxis();

        List <Integer> checkFieldIndex = new ArrayList<>();
        xAxis.stream().forEach(field->checkFieldIndex.add(field.getColumnIndex()));
        yAxis.stream().forEach(field->checkFieldIndex.add(field.getColumnIndex()));

        List <String []> returnList = new ArrayList<>();

        dataListAfterFilter.forEach(str->{
            String [] returnStr = new String[checkFieldIndex.size()];
            for (int i = 0 ; i < checkFieldIndex.size() ; i++) {
                returnStr[i] = str[checkFieldIndex.get(i)];
            }
            returnList.add(returnStr);
        });


        return returnList;

    }



        private Map<String, List>  mergePiAndSqlServerDataAndField (Map<String, List> piApiStringListMap , Map<String, List> sqlServerStringListMap ) {

        // 合并数据
        // 字段
        List fieldList = piApiStringListMap.get("fieldList");
        fieldList.addAll(sqlServerStringListMap.get("fieldList"));
        // 数据
        List<String[]> piDataList = piApiStringListMap.get("dataList");
        List<String[]> sqlServerDataList = sqlServerStringListMap.get("dataList");

        //取 PI 数据里的第一条做比对
        Optional<String[]> first = sqlServerDataList.stream().filter(sqlServerData -> sqlServerData[2].equals(piDataList.get(0)[3])).findFirst();

        List<String[]> piDataListAfter = new ArrayList<>();

        if (first.isPresent()) {
            String[] strings = first.get();
            piDataList.forEach(piData ->  {
                piData = ArrayUtils.addAll(piData ,strings);
                piDataListAfter.add(piData);
            });
        } else {
            piDataList.forEach(piData -> {
                piData = ArrayUtils.addAll(piData , " "," "," ");
                piDataListAfter.add(piData);
            });
        }
        piApiStringListMap.put("dataList" ,piDataListAfter );

        return piApiStringListMap;

    }



    public Map<String, List> fetchSqlServerResultAndField( DatasourceRequest datasourceRequest) throws Exception {
        //塞入 sql server 里的数据
        // sql server 数据
        datasourceRequest.getDatasource().setType("sqlServer");
        JSONObject jsonObject = JSONObject.parseObject(datasourceRequest.getDatasource().getConfiguration());
        datasourceRequest.setQuery(jsonObject.getString("sql"));
        Map<String, List> sqlServerStringListMap = jdbcProvider.fetchResultAndField(datasourceRequest);

        return sqlServerStringListMap;
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

        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        if (CollectionUtils.isNotEmpty(chartExtFilterRequests)) {
            chartExtFilterRequests.forEach(chartExtFilterRequest -> {
                if (chartExtFilterRequest.getOperator().equals("between")) {
                    //String format = f.format((Long.valueOf(chartExtFilterRequest.getValue().get(0))));
                    start_time_filter[0] = f.format((Long.valueOf(chartExtFilterRequest.getValue().get(0))));
                    end_time_filter[0] = f.format((Long.valueOf(chartExtFilterRequest.getValue().get(1))));
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


        Date endDate = new Date();
        Date startDate = DateUtils.addHours(endDate ,  -1);


        String url = config.getString("url");
        String point_name = requestInfo.getString("point_name");
        String data_rate = StringUtils.isEmpty(data_rate_filter[0])? ( StringUtils.isEmpty(requestInfo.getString("data_rate"))? "60" : requestInfo.getString("data_rate") ): data_rate_filter[0];
        String start_time =  StringUtils.isEmpty(start_time_filter[0])? ( StringUtils.isEmpty(requestInfo.getString("start_time"))?f.format(startDate):requestInfo.getString("start_time")): start_time_filter[0];
        String end_time = StringUtils.isEmpty(end_time_filter[0])? ( StringUtils.isEmpty(requestInfo.getString("end_time")) ? f.format(endDate):requestInfo.getString("end_time")  ) : end_time_filter[0];

        JSONObject jsonObject = new JSONObject();
        JSONArray pointData = new JSONArray();
        JSONObject onepoint = new JSONObject();
        onepoint.put("PointCode" , point_name);
        onepoint.put("DataRate" , data_rate);
        onepoint.put("Position" , "First");
        onepoint.put("StartTime" , start_time);
        onepoint.put("EndTime" , end_time);
        pointData.add(onepoint);
        jsonObject.put("PointData" , pointData);
        jsonObject.put("dpCounts" , 2);

        String response = HttpClientUtil.post(url+"/AFPI/History/GetRecordRangeValues", jsonObject.toJSONString(), httpClientConfig);

        JSONObject resp = JSONObject.parseObject(response);

        JSONArray resultData = resp.getJSONArray("ResultData");
        JSONArray dataValues = resultData.getJSONObject(0).getJSONArray("DataValue");

        return dataValues;
    }

    public Map<String, List> fetchResultAndField(String  configuration , String info ,List<ChartExtFilterRequest> chartExtFilterRequests) throws Exception {

        JSONArray dataValues = executePiApiQuery(configuration, info , chartExtFilterRequests);

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

        // sql server 数据
        datasourceRequest.getDatasource().setType("sqlServer");
        JSONObject jsonObject = JSONObject.parseObject(datasourceRequest.getDatasource().getConfiguration());
        datasourceRequest.setQuery(jsonObject.getString("sql"));
        Map<String, List> sqlServerStringListMap = jdbcProvider.fetchResultAndField(datasourceRequest);

        // pi 数据
        datasourceRequest.getDatasource().setType("pi_api");
        Map<String, List> piApiStringListMap = fetchResultAndField(datasourceRequest.getDatasource().getConfiguration(), datasourceRequest.getInfo() ,null);

        return mergePiAndSqlServerDataAndField(piApiStringListMap ,sqlServerStringListMap );
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

        // sql server 数据
        datasourceRequest.getDatasource().setType("sqlServer");
        JSONObject jsonObject = JSONObject.parseObject(datasourceRequest.getDatasource().getConfiguration());
        datasourceRequest.setQuery(jsonObject.getString("sql"));
        Map<String, List> sqlServerStringListMap = jdbcProvider.fetchResultAndField(datasourceRequest);

        List <TableField>fieldList = sqlServerStringListMap.get("fieldList");
        fieldList.forEach(field -> {
            if (field.getFieldName().equals("AlarmUpper")) {
                field.setFieldType("3");
            } else if (field.getFieldName().equals("AlarmLower")) {
                field.setFieldType("3");
            } else {
                field.setFieldType("0");
            }
        });


        List<TableField> tableFields = getTableFields();
        tableFields.addAll(fieldList);

        return tableFields;
    }

    public String checkStatus(DatasourceRequest datasourceRequest) throws Exception {
        // pi api
        getOauthToken(datasourceRequest);
        // sql server
        datasourceRequest.getDatasource().setType("sqlServer");
        jdbcProvider.checkStatus(datasourceRequest);
        datasourceRequest.getDatasource().setType("pi_api");
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
        for (Map<String, String> header : apiDefinitionRequest.getHeaders()) {
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



}
