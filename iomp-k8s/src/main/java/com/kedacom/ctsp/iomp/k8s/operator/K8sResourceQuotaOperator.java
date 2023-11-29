package com.kedacom.ctsp.iomp.k8s.operator;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceQuota;
import io.fabric8.kubernetes.api.model.ResourceQuotaBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class K8sResourceQuotaOperator {

    private final K8sClientHolder k8sClientHolder;

    public K8sResourceQuotaOperator(K8sClientHolder k8sClientHolder) {
        this.k8sClientHolder = k8sClientHolder;
    }

    public static final String RESOURCE_QUOTA_PREFIX = "-resource-quota";

    private int NAMESPACE_CTRL = 1;

    public void createOrReplace(String namespaceName, String limitCpu, String limitMemory, Integer cpuCtrl, Integer memoryCtrl) {
        log.info("update => namespaceName:{},limitCpu:{},limitMemory{},cpuCtrl:{},memoryCtrl:{}", namespaceName, limitCpu, limitMemory, cpuCtrl, memoryCtrl);
        String quotaName = namespaceName + RESOURCE_QUOTA_PREFIX;
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        ResourceQuota resourceQuota = client.resourceQuotas().inNamespace(namespaceName).withName(quotaName).get();
        if (resourceQuota == null) {
            resourceQuota = new ResourceQuotaBuilder().withNewMetadata()
                    .withName(quotaName)
                    .withNamespace(namespaceName)
                    .endMetadata()
                    .withNewSpec()
                    .endSpec()
                    .build();
        }
        Map<String, Quantity> hard = resourceQuota.getSpec().getHard();
        if (hard == null) {
            hard = Maps.newHashMap();
        }
        Map<String, String> annotations = Maps.newHashMap();
        if (cpuCtrl == NAMESPACE_CTRL) {
            hard.put("limits.cpu", new Quantity(limitCpu));
            annotations.put("cpu", limitCpu);
        } else {
            hard.remove("limits.cpu");
        }
        if (memoryCtrl == NAMESPACE_CTRL) {
            hard.put("limits.memory", new Quantity(limitMemory + "Mi"));
            annotations.put("memory", limitMemory);
        } else {
            hard.remove("limits.memory");
        }
        resourceQuota.getMetadata().setAnnotations(annotations);
        resourceQuota.getSpec().setHard(hard);
        Long startTime = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.info("resourceQuota: " + JSON.toJSONString(resourceQuota));
        }
        client.resourceQuotas().inNamespace(namespaceName).createOrReplace(resourceQuota);
    }


    public void patch(String namespaceName, String limitCpu, String limitMemory, Integer cpuCtrl, Integer memoryCtrl) {
        log.info("update => namespaceName:{},limitCpu:{},limitMemory{},cpuCtrl:{},memoryCtrl:{}", namespaceName, limitCpu, limitMemory, cpuCtrl, memoryCtrl);
        String quotaName = namespaceName + RESOURCE_QUOTA_PREFIX;
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        ResourceQuota resourceQuota = client.resourceQuotas().inNamespace(namespaceName).withName(quotaName).get();
        if (resourceQuota == null) {
            resourceQuota = new ResourceQuotaBuilder().withNewMetadata()
                    .withName(quotaName)
                    .withNamespace(namespaceName)
                    .endMetadata()
                    .withNewSpec()
                    .endSpec()
                    .build();
        }
        Map<String, Quantity> hard = resourceQuota.getSpec().getHard();
        if (hard == null) {
            hard = Maps.newHashMap();
        }
        Map<String, String> annotations = resourceQuota.getMetadata().getAnnotations() == null ? Maps.newHashMap() : resourceQuota.getMetadata().getAnnotations();
        if (cpuCtrl != null) {
            if (cpuCtrl == NAMESPACE_CTRL) {
                hard.put("limits.cpu", new Quantity(limitCpu));
                annotations.put("cpu", limitCpu);
            } else {
                hard.remove("limits.cpu");
            }
        }

        if (memoryCtrl != null) {
            if (memoryCtrl == NAMESPACE_CTRL) {
                hard.put("limits.memory", new Quantity(limitMemory + "Mi"));
                annotations.put("memory", limitMemory);
            } else {
                hard.remove("limits.memory");
            }
        }

        resourceQuota.getMetadata().setAnnotations(annotations);
        resourceQuota.getSpec().setHard(hard);
        Long startTime = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.info("resourceQuota: " + JSON.toJSONString(resourceQuota));
        }
        client.resourceQuotas().inNamespace(namespaceName).createOrReplace(resourceQuota);
    }


    public void apiCreateOrReplace(String namespaceName, String limitCpu, String limitMemory, Integer cpuCtrl, Integer memoryCtrl) {
        log.info("update => namespaceName:{},limitCpu:{},limitMemory{},cpuCtrl:{},memoryCtrl:{}", namespaceName, limitCpu, limitMemory, cpuCtrl, memoryCtrl);
        String quotaName = namespaceName + RESOURCE_QUOTA_PREFIX;
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        ResourceQuota resourceQuota = client.resourceQuotas().inNamespace(namespaceName).withName(quotaName).get();
        if (resourceQuota == null) {
            resourceQuota = new ResourceQuotaBuilder().withNewMetadata()
                    .withName(quotaName)
                    .withNamespace(namespaceName)
                    .endMetadata()
                    .withNewSpec()
                    .endSpec()
                    .build();
        }
        Map<String, Quantity> hard = resourceQuota.getSpec().getHard();
        if (hard == null) {
            hard = Maps.newHashMap();
        }
        Map<String, String> annotations = Maps.newHashMap();
        if (null != cpuCtrl) {
            if (cpuCtrl == NAMESPACE_CTRL) {
                hard.put("limits.cpu", new Quantity(limitCpu));
                annotations.put("cpu", limitCpu);
            } else {
                hard.remove("limits.cpu");
            }
        }
        if (null != memoryCtrl) {
            if (memoryCtrl == NAMESPACE_CTRL) {
                hard.put("limits.memory", new Quantity(limitMemory + "Mi"));
                annotations.put("memory", limitMemory);
            } else {
                hard.remove("limits.memory");
            }
        }
        resourceQuota.getMetadata().setAnnotations(annotations);
        resourceQuota.getSpec().setHard(hard);
        Long startTime = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.info("resourceQuota: " + JSON.toJSONString(resourceQuota));
        }
        client.resourceQuotas().inNamespace(namespaceName).createOrReplace(resourceQuota);
    }

    public ResourceQuota getResourceQuotaByNamespaceName(String namespaceName) {
        log.info("get => namespaceName:{}", namespaceName);
        String quotaName = namespaceName + RESOURCE_QUOTA_PREFIX;
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        return client.resourceQuotas().inNamespace(namespaceName).withName(quotaName).get();
    }
}
