package com.kedacom.ctsp.iomp.k8s.vo;

import lombok.Data;

import java.util.Map;

/**
 * @author tangjixing
 * @date 2020/6/4
 */
@Data
public class K8sPvVo {
    /**
     * 名称
     */
    private String name;

    /**
     * 命名空间
     */
    private String namespace;


    private String storageClassName;

    /**
     * 空间大小
     */
    private Long storageSize;


    private Map<String, String> labels;


    private String hostname;

    private String path ;

    private boolean waitForFirstConsumer = false;

}
