package com.kedacom.ctsp.iomp.k8s.vo;

import lombok.Data;

/**
 *  k8s 创建pvc,pv vo
 *
 * @author hef
 **/
@Data
public class K8sPvPvcVo {

    /**
     * 名称
     */
    private String name;

    /**
     * 命名空间
     */
    private String nameSpace;

    /**
     * 空间大小
     */
    private String storageSize;

    /**
     * 指定class类型
     */
    private String storageClassName;

    /**
     * nfs服务器上对应的目录
     */
    private String nfsServerPath;

    /**
     * nfs服务器
     */
    private String nfsServer;



}
