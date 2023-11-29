package com.kedacom.ctsp.iomp.k8s.vo;

import io.fabric8.kubernetes.api.model.ServicePort;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * k8s service 部署信息
 *
 * @author hef
 **/
@Data
public class K8sServiceDeployVo {

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
     * 类型
     */
    private String type;
    /**
     * 服务端口集合
     */
    private List<ServicePort> ports;
    /**
     * 选择器
     */
    private Map<String, String> selector;












}
