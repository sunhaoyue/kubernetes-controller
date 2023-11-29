package com.kedacom.ctsp.iomp.k8s.vo;

import io.fabric8.kubernetes.api.model.extensions.IngressRule;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * k8s Ingress 部署信息
 *
 * @author hef
 **/
@Data
public class K8sIngressVo {

    /**
     * 命名空间
     */
    private String nameSpace;
    /**
     * 任务名
     */
    private String name;
    /**
     * API版本
     */
    private String apiVersion;
    /**
     * 注解
     */
    private Map<String, String> annotations;
    /**
     * ingress rules
     *
     */
    private List<IngressRule> rules;










}
