package com.kedacom.ctsp.iomp.k8s;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kedacom.ctsp.iomp.k8s.operator.K8sConfigmapOperator;
import io.fabric8.kubernetes.api.model.ConfigMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author tangjixing
 * @date 2019/11/7
 */
@Slf4j
public class K8sConfigmapOperatorTest {

    private K8sConfig k8sConfig;
    private K8sClientHolder clientHolder;

    private String namespace = "kedacom-project-namespace";

    @Before
    public void setup() {
        k8sConfig = new K8sConfig();
        k8sConfig.setProtocol("https");
        k8sConfig.setMasters(Arrays.asList("10.68.7.151:6443", "10.68.7.152:6443", "10.68.7.153:6443"));
        k8sConfig.setCaCertFile("D:/data/k8s/ca.crt");
        k8sConfig.setClientCertFile("D:/data/k8s/apiserver-kubelet-client.crt");
        k8sConfig.setClientKeyFile("D:/data/k8s/apiserver-kubelet-client.key");

        clientHolder = new K8sClientHolder(k8sConfig);
    }

    @Test
    public void testCreateOrReplaceConfigmap() throws JsonProcessingException {
        K8sConfigmapOperator deployOperator = new K8sConfigmapOperator(clientHolder);
        String configMapName = "test-global-config";
        Map<String, String> data = new HashMap<>();
        data.put("vip", "10.68.7.1");
        deployOperator.createOrReplaceConfigmap(configMapName, namespace, data);
        Map<String, String> dataAppend = new HashMap<>();
        dataAppend.put("vip", "10.68.7.2");
        dataAppend.put("ingress", "10.68.7.99");
        deployOperator.createOrReplaceConfigmap(configMapName, namespace, dataAppend, true);

        ConfigMap configMap = deployOperator.get(configMapName, namespace);
        assertEquals(configMap.getData().size(), 2);
        assertEquals(configMap.getData().get("vip"), "10.68.7.1");
        assertEquals(configMap.getData().get("ingress"), "10.68.7.99");

        assertTrue(deployOperator.deleteConfigMap(configMapName, namespace));
        assertFalse(deployOperator.deleteConfigMap(configMapName, namespace));
    }





}
