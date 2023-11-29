package com.kedacom.ctsp.iomp.k8s;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.Data;

import java.util.List;

/**
 * kubernetes 配置
 * <p>
 * Created by hefei on 2017/2/28.
 */
@Data
public class K8sConfig {

    /**
     * 集群名称 作为k8s连接失败使用提示
     */
    private String k8sName;

    /**
     * 协议： http  https
     */
    private String protocol;

    /**
     * k8s master
     */
    private List<String> masters;

    /**
     * 以下属性只有https方式才有
     */
    private String caCertFile;

    private String clientCertFile;

    private String clientKeyFile;

    public static void main(String[] args) {
        Config config = new ConfigBuilder().withMasterUrl("https://10.68.7.156:6443")
                .withCaCertFile("C:/Users/Administrator/Downloads/ca.crt")
                .withClientCertFile("C:/Users/Administrator/Downloads/apiserver-kubelet-client.crt")
                .withClientKeyFile("C:/Users/Administrator/Downloads/apiserver-kubelet-client.key").build();

        DefaultKubernetesClient client = new DefaultKubernetesClient(config);
        System.out.println("123"+ client.getVersion().toString());
    }

}
