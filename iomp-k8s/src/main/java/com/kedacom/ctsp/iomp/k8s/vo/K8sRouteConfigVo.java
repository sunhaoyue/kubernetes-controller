package com.kedacom.ctsp.iomp.k8s.vo;

import lombok.Data;

@Data
public class K8sRouteConfigVo {

    /**
     * 服务名称 - 服务标识
     */
    private String serviceName;
    /**
     * 名称 - 路由名称
     */
    private String name;
    /**
     * 协议 http,https,tcp
     */
    private String protocol;
    /**
     * 主机列表
     */
    private String hostList;
    /**
     * 路径
     */
    private String path;
    /**
     * 方法
     */
    private String method;
    /**
     * 匹配优先级 默认值为【0】
     */
    private Integer regexPriority=0;
    /**
     * 剥去路径 0:否 1：是
     */
    private Integer stripPath;
    /**
     * 黑白名单标志位 0：白名单 1：黑名单
     */
    private Integer whiteOrBlack;
    /**
     *白名单
     */
    private String whiteList;
    /**
     * 黑名单
     */
    private String blackList;
    /**
     * snis
     */
    private String snis;
    /**
     * sources
     */
    private String sources;
    /**
     * destinations
     */
    private String destinations;
    /**
     * 是否已经生效
     */
    private Integer flag;
    /**
     * 路由到的服务端口
     */
    private Integer port;
    /**
     * 项目标识
     */
    private String projectSign;
}
