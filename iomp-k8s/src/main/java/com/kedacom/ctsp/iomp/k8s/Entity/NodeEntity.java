package com.kedacom.ctsp.iomp.k8s.Entity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 主机基本信息 entity
 *
 * @author hefei
 * @date 2018/12/29
 */
@Data
@Slf4j
public class NodeEntity {

    /**
     * - nodename: node-05b2
     *         hostIp: "10.165.34.159"
     *         sshAuthType: passwd
     *         port: 2222
     *         username: "keda"
     *         password: "Ked@com#123"
     *         role: "master"
     */
    private String hostIp;
    private String sshAuthType="passwd";
    private String username;
    private String password;
    private long port;

    private String nodeName;
    /**
     * 集群node的name
     */
    private String k8sNodeName;



    /**
     * 节点类型
     * 字典:  0 主节点，1从节点
     */
    private long nodeType;

    /**
     * 1：meta主机    0：一般主机
     */
    private long isMeta;

    /**
     * 0:单节点, 1:高可用
     */
    private long nodeSchemeType;

    /**
     * 集群id
     */
    private String clusterId;



}
