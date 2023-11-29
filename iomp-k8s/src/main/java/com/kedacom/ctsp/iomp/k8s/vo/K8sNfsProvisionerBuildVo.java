package com.kedacom.ctsp.iomp.k8s.vo;

import lombok.Data;

/**
 *  k8s vo
 *
 * @author hef
 **/
@Data
public class K8sNfsProvisionerBuildVo {

    /**
     * 命名空间
     */
    private String nameSpace;

    /**
     * 复制几个pods
     */
    private Integer replicas;

    /**
     * 任务名
     */
    private String name;

    /**
     * 镜像名
     */
    private String image;

    private String nfsServer;

    private String nfsPath;

    private String mountName;
    private String mountPath;

}
