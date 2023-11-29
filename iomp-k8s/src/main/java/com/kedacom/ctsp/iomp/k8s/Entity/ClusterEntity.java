package com.kedacom.ctsp.iomp.k8s.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;


/**
 * 具体的组件，包含具体的组件包，helm包，曲线等
 */
@Data
public class ClusterEntity {

    private String id;

    /**
     * 集群名称
     */
    private String k8sName;

    /**
     * 集群标识
     */
    private String k8sSign;

    /**
     * service网段
     */
    private String serviceSubnet;

    /**
     *
     */
    private String corednsType;
    /**
     * 脚本类型： normal-正常模式，hardcore-极限模式
     */
    private String scriptType;

    private String clusterType;

    /**
     * todo 集群版本，comfigMap获取
     */
    private String clusterVersion;
    /**
     * k8s CA证书文件跟路径
     * /data/iomp_data/kube/7becb15a122b4b07b52a26ad8ca8b1a8/
     */
    private String kubeRoot;
    /**
     * ?? todo
     */
    private String certRoot;
    /**
     * todo
     */
    private String sshKey;

    private long status;

     /**
     * k8s CA证书文件地址
     * /data/iomp_data/kube/f9386e54027d48e7ad6bcb8fc3e5c1a0/etc/kubernetes/pki/ca.crt
     */
    private String caCertFile;

    /**
     * k8s 客户端证书文件地址
     * /data/iomp_data/kube/f9386e54027d48e7ad6bcb8fc3e5c1a0/etc/kubernetes/pki/apiserver-kubelet-client.crt
     */
    private String clientCertFile;

    /**
     * k8s 客户端key文件地址
     * /data/iomp_data/kube/f9386e54027d48e7ad6bcb8fc3e5c1a0/etc/kubernetes/pki/apiserver-kubelet-client.key
     */
    private String clientKeyFile;
    private String registryUrl;
    private String registryEnv;

    private String workMode;



}
