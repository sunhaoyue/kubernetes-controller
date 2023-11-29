package com.kedacom.ctsp.iomp.k8s.vo;


import lombok.Data;

@Data
public class K8sGatewayVO {

    private String name;

    private String namespace;

    public static K8sGatewayVO instance(String name, String namespace) {
        K8sGatewayVO vo = new K8sGatewayVO();
        vo.setName(name);
        vo.setNamespace(namespace);
        return vo;
    }
}
