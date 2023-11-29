package com.kedacom.ctsp.iomp.k8s.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * k8s 创建pvc
 *
 * @author hef
 **/
@Data
@Accessors(chain = true)
public class K8sPvcVo {

    /**
     * 名称
     */
    private String name;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 空间大小
     */
    private String storageSize;

    /**
     * 指定class类型
     */
    private String storageClassName;

    private String client_id;

    private String pvName;
}
