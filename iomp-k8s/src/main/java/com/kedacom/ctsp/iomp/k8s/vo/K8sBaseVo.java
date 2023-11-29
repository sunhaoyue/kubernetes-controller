package com.kedacom.ctsp.iomp.k8s.vo;

import lombok.Data;

import java.util.Map;

/**
 * @author tangjixing
 * @date 2019/11/22
 */
@Data
public class K8sBaseVo {
    private String name;

    private String namespace;

    private String kind;

    private Map<String, String> labels;

    private Map<String, String> annotations;
}
