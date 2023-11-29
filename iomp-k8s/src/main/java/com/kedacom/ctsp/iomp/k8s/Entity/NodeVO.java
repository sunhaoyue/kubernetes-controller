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
public class NodeVO {

    /**
     * - nodename: node-05b2
     *
     *        hostIp: "10.165.34.159"
     *         sshAuthType: passwd
     *         port: 2222
     *         username: "keda"
     *         password: "Ked@com#123"
     *         role: "master"
     */
    /**
     * 集群node的name
     */
    private String nodename;
    private String hostIp;
    private String sshAuthType="passwd";
    private String username;
    private String password;
    private long port;

    /**
     * 1：meta主机    0：一般主机
     */
    private String role;







}
