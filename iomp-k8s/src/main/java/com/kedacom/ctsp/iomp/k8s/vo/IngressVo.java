package com.kedacom.ctsp.iomp.k8s.vo;

import lombok.Data;

/**
 * k8s Ingress 部署信息
 *
 * @author hef
 **/
@Data
public class IngressVo {

    /**
     * 命名空间
     */
    private String nameSpace;

    private String ingressName;
    private String path;
    private Integer ingressPort;

}
