package com.kedacom.ctsp.iomp.k8s.vo;


import lombok.Data;

@Data
public class K8sLocalpvVo {

    private String clusterId;

    /**
     * 服务名称
     */
    private String k8sName;

    /**
     * 空间
     */
    private String namespace;

    /**
     * 主机目录
     */
    private String hostPath;


    /**
     * 容器挂载目录
     */
    private String mountPath;

    /**
     * 主机列表，以逗号隔开
     */
    private String hostIps;

}
