package io.dataease.controller.authModel;

import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.dataease.controller.request.authModel.VAuthModelRequest;
import io.dataease.dto.authModel.VAuthModelDTO;
import io.dataease.service.authModel.VAuthModelService;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * Author: wangjiahao
 * Date: 2021/11/5
 * Description:
 */
@Api(tags = "授权树：授权树模型")
@ApiSupport(order = 80)
@RestController
@RequestMapping("authModel")
public class VAuthModelController {

    @Resource
    private VAuthModelService vAuthModelService;

    @PostMapping("/queryAuthModel")
    public List<VAuthModelDTO> queryAuthModel(@RequestBody VAuthModelRequest request){
        return vAuthModelService.queryAuthModel(request);
    }

    @PostMapping("/queryAuthModel/{datasourceId}")
    public List<VAuthModelDTO> queryAuthModelByDatasourceId(@PathVariable String datasourceId ){
        return vAuthModelService.listByDatasourceId(datasourceId);
    }



}
