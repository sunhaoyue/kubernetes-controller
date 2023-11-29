package com.kedacom.ctsp.iomp.k8s.vo;

import lombok.Data;

import java.util.Map;

/**
 * @author tangjixing
 * @date 2019/11/22
 */
@Data
public class K8sConfigMapVo extends K8sBaseVo {

    private String kind = "configMap";
    private Map<String, String> data;

}
