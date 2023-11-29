package com.kedacom.ctsp.iomp.k8s.operator;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import com.kedacom.ctsp.iomp.k8s.common.cache.K8sAbstractOperator;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.scheduling.v1beta1.PriorityClass;
import io.fabric8.kubernetes.api.model.scheduling.v1beta1.PriorityClassBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.kedacom.ctsp.iomp.k8s.constant.K8sPriorityClassConstant.*;

@Slf4j
public class K8sPriorityClassOperator extends K8sAbstractOperator {

    public K8sPriorityClassOperator(K8sClientHolder k8sClientHolder) {
       super(k8sClientHolder);
    }

    /**
     * 如果没有就创建
     */
    public void createIfAbsent() {
        List<String> prioritys = Lists.newArrayList(L1_NAME, L2_NAME, L3_NAME, L4_NAME);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        for (String priority : prioritys) {
            PriorityClass priorityClass = client.scheduling().priorityClass().withName(priority).get();
            if (priorityClass != null) {
                continue;
            }
            priorityClass = new PriorityClassBuilder().withNewMetadata()
                    .withName(priority)
                    .endMetadata()
                    .withValue(priorityMap.get(priority))
                    .build();
            if (log.isDebugEnabled()) {
                log.debug("priorityClass: " + JSON.toJSONString(priorityClass));
            }
            client.scheduling().priorityClass().createOrReplace(priorityClass);
            this.patchCache(priorityClass);
        }
    }

    //------------------------------------------------------------------------以下为重构方法--------------------------------/
    public List<io.fabric8.kubernetes.api.model.scheduling.v1beta1.PriorityClass> list() {
        log.info("get => list all");
        return k8sClientHolder.getClient().scheduling().priorityClass().list().getItems();

    }

    @Override
    public List<io.fabric8.kubernetes.api.model.scheduling.v1beta1.PriorityClass> list(String namespace) {
        return this.list();
    }

    public List<io.fabric8.kubernetes.api.model.scheduling.v1beta1.PriorityClass> list(Long limit) {

        // 若值为空或小于0则默认100条
        if (limit == null || limit < 0) {
            return this.list();
        }
        log.info("get => limit:{}", limit);
        return k8sClientHolder.getClient().scheduling().priorityClass().list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    /**
     * 根据名称获取对象
     * @param name
     * @return
     */
    public io.fabric8.kubernetes.api.model.scheduling.v1beta1.PriorityClass get(String name) {
        log.info("get => name:{}", name);
        return k8sClientHolder.getClient().scheduling().priorityClass().withName(name).get();
    }
}
