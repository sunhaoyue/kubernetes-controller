package com.kedacom.ctsp.iomp.k8s.vo;

import lombok.Data;

import java.util.Map;

/**
 * k8s 创建 StorageClass
 *
 * @author hef
 **/
@Data
public class K8sStorageClassVo {

    /**
     * 名称
     */
    private String name;

    /**
     * 命名空间
     */
    private String nameSpace;

    /**
     * 供应者
     */
    private String provisioner;

    /**
     * 是否需要文件锁
     */
    private Boolean nfsLock;

    private String mountOptions;

    private Map<String, String> annotations;
}
