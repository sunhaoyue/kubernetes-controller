package com.kedacom.ctsp.iomp.k8s.operator;

import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import com.kedacom.ctsp.iomp.k8s.common.cache.K8sAbstractOperator;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

@Slf4j
public class K8sPodOperator extends K8sAbstractOperator {

    protected K8sClientHolder getK8sClientHolder() {
        return k8sClientHolder;
    }

    public K8sPodOperator(K8sClientHolder k8sClientHolder) {
        super(k8sClientHolder);
    }

    public List<Pod> listNotRunning() {
        return k8sClientHolder.getClient().pods().inAnyNamespace().withoutField("status.phase", "Running").list().getItems();
    }

    public boolean delete(String name, String namespace) {
        log.info("delete => name:{},namespace:{}", name, namespace);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        boolean result = client.pods().inNamespace(namespace).withName(name).delete();
//        k8sClientHolder.getClient().pods().inNamespace(namespace).withName(name).waitUntilCondition(t->"Succeeded".equals(t.getStatus().getPhase()),600, TimeUnit.SECONDS);

        if (result) {
            this.delCache(name, namespace);
        }
        return result;
    }

    public boolean delete(String namespace, Map<String, String> labels) {
        log.info("delete => namespace:{},labels:{}", namespace, labels);
        return k8sClientHolder.getClient().pods().inNamespace(namespace).withLabels(labels).delete();
    }

    public Boolean deleteWithLabelIn(String namespace, String key, String... values) {
        log.info("delete => namespace:{},key:{},values:{}", namespace, key, values);
        boolean result;
        if (StringUtils.isNotBlank(namespace)) {
            result = k8sClientHolder.getClient().pods().inNamespace(namespace).withLabelIn(key, values).delete();
        } else {
            result = k8sClientHolder.getClient().pods().withLabelIn(key, values).delete();
        }
        // 待验证
//        k8sClientHolder.getClient().pods().inNamespace(namespace).withLabelIn(key, values).list().waitUntilCondition(t->"Succeeded".equals(t.getStatus().getPhase()),600, TimeUnit.SECONDS);
        if (result) {
            this.delCacheByLabel(namespace, key, values);
        }
        return result;
    }

    public void createOrReplace(String namespace, Pod pod) {
        k8sClientHolder.getClient().pods().inNamespace(namespace).createOrReplace(pod);
    }

    //------------------------------------------------------------------------以下为重构方法--------------------------------/
    @Override
    public Pod toSimpleData(HasMetadata hasMetadata) {
        Pod pod = (Pod) hasMetadata;
        pod.getMetadata().setResourceVersion(null);
        pod.getMetadata().setSelfLink(null);
        pod.getMetadata().setUid(null);
        pod.getMetadata().setManagedFields(null);
        pod.getMetadata().setOwnerReferences(null);

        for (Container container : pod.getSpec().getContainers()) {
            container.setEnv(null);
            container.setCommand(null);
            container.setVolumeMounts(null);
            container.setReadinessProbe(null);
            container.setLivenessProbe(null);
            container.setArgs(null);
            container.setReadinessProbe(null);
        }

        for (Container initContainer : pod.getSpec().getInitContainers()) {
            initContainer.setEnv(null);
            initContainer.setCommand(null);
            initContainer.setVolumeMounts(null);
            initContainer.setReadinessProbe(null);
            initContainer.setLivenessProbe(null);
            initContainer.setArgs(null);
            initContainer.setReadinessProbe(null);
        }

        pod.getStatus().setConditions(null);
        //pod.getStatus().setContainerStatuses(null);
        return pod;
    }

    @Override
    public List<Pod> list() {
        log.info("get => list all");
        return k8sClientHolder.getClient().pods().inAnyNamespace().list().getItems();
    }

    @Override
    public List<Pod> list(String namespace) {

        if (StringUtils.isBlank(namespace)) {
            return this.list();
        }
        log.info("get => namespace:{}", namespace);
        return k8sClientHolder.getClient().pods().inNamespace(namespace).list().getItems();
    }

    public List<Pod> list(Long limit) {
        log.info("get => limit:{}", limit);
        return k8sClientHolder.getClient().pods().inAnyNamespace().list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    public List<Pod> list(String namespace, Long limit) {

        // 若值为空或小于0则默认100条
        if (limit == null || limit < 0) {
            return this.list(namespace);
        }

        // 空间为空时，查询当前集群下所有信息
        if (StringUtils.isBlank(namespace)) {
            return this.list(limit);
        }
        log.info("get => namespace:{} ,limit:{}", namespace, limit);
        return k8sClientHolder.getClient().pods().inNamespace(namespace).list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    /**
     * 根据名称获取对象
     *
     * @param namespace 必填，否则无法查询name
     * @param name
     * @return
     */
    public Pod get(String name, String namespace) {
        log.info("get => namespace:{} ,name:{}", namespace, name);
        return k8sClientHolder.getClient().pods().inNamespace(namespace).withName(name).get();
    }

    public List<Pod> listByLabels(Map<String, String> labels) {
        log.info("get => labels:{}", labels);
        return k8sClientHolder.getClient().pods().inAnyNamespace().withLabels(labels).list().getItems();
    }

    public List<Pod> listByLabels(String namespace, Map<String, String> labels) {
        log.info("get => namespace:{},labels:{}", namespace, labels);
        if (StringUtils.isEmpty(namespace)) {
            return this.listByLabels(labels);
        }
        return k8sClientHolder.getClient().pods().inNamespace(namespace).withLabels(labels).list().getItems();
    }

    public List<Pod> listByLabelCustom(String label, String... value) {
        log.info("get => label:{},value{}", label, value);
        return k8sClientHolder.getClient().pods().withLabelIn(label, value).list().getItems();
    }
}
