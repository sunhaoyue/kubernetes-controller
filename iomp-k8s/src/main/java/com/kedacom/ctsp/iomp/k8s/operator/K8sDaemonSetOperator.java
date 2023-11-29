package com.kedacom.ctsp.iomp.k8s.operator;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import com.kedacom.ctsp.iomp.k8s.common.cache.K8sAbstractOperator;
import com.kedacom.ctsp.iomp.k8s.constant.K8sConstants;
import com.kedacom.ctsp.iomp.k8s.convert.DaemonSetConvert;
import com.kedacom.ctsp.iomp.k8s.dto.DeploymentEditDto;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class K8sDaemonSetOperator extends K8sAbstractOperator {

    public K8sDaemonSetOperator(K8sClientHolder k8sClientHolder) {
        super(k8sClientHolder);
    }

    public void createOrReplace(DaemonSet daemonSet) {
        log.info("update => DaemonSet:{}", JSON.toJSONString(daemonSet));
        k8sClientHolder.getClient().apps().daemonSets().inNamespace(daemonSet.getMetadata().getNamespace()).createOrReplace(daemonSet);
        this.patchCache(daemonSet);
    }

    public void createOrReplace(String namespace, DaemonSet daemonSet) {
        log.info("update => namespace:{},daemonSet:{}", namespace, JSON.toJSONString(daemonSet));
        k8sClientHolder.getClient().apps().daemonSets().inNamespace(namespace).createOrReplace(daemonSet);
        this.patchCache(daemonSet);
    }


    public boolean createOrReplace(String name, String namespace, String yaml) {
        log.info("update => name:{},namespace:{},yaml:{}", name, namespace, yaml);
        try {
            DefaultKubernetesClient client = k8sClientHolder.getClient();
            DaemonSet daemonSet = client.apps().daemonSets().inNamespace(namespace).withName(name).get();
            if (daemonSet != null) {
                Representer representer = new Representer();
                representer.getPropertyUtils().setSkipMissingProperties(true);
                Yaml yamlObj = new Yaml(new Constructor(DaemonSet.class), representer);
                DaemonSet newDaemonSet = yamlObj.loadAs(yaml, DaemonSet.class);
                newDaemonSet.getMetadata().setResourceVersion(null);
                newDaemonSet.setApiVersion(K8sConstants.API_VERSION_DEPLOYMENT);
                if (log.isDebugEnabled()) {
                    log.debug("daemonSet: " + JSON.toJSONString(newDaemonSet));
                }
                client.apps().daemonSets().inNamespace(namespace).createOrReplace(newDaemonSet);
                this.patchCache(newDaemonSet);
                return true;
            }
        } catch (Exception e) {
            log.error("patch daemonset by yaml found exception, daemonset:{}, namespace:{}, yaml:{}", name, namespace, yaml, e);
        }
        return false;
    }

    public boolean delete(String name, String namespace) {
        log.info("delete => name:{},namespace:{}", name, namespace);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        boolean result = client.apps().daemonSets().inNamespace(namespace).withName(name).cascading(true).delete();
        if (result) {
            this.delCache(name, namespace);
        }
        return result;
    }

    public Boolean deleteWithLabelIn(String namespace, String key, String... values) {
        log.info("delete => namespace:{},key:{},values:{}", key, values);
        boolean result;
        if (StringUtils.isNotBlank(namespace)) {
            result = k8sClientHolder.getClient().apps().daemonSets().inNamespace(namespace).withLabelIn(key, values).delete();
        } else {
            result = k8sClientHolder.getClient().apps().daemonSets().withLabelIn(key, values).delete();
        }
        if (result) {
            this.delCacheByLabel(namespace, key, values);
        }
        return result;
    }

    public void patch(String deployName, String namespace, DeploymentEditDto editDto) {
        log.info("update => deployName:{},namespace:{},editDto{}", deployName, namespace, JSON.toJSONString(editDto));
        DaemonSet daemonSet = get(deployName, namespace);
        daemonSet = new DaemonSetConvert().merge(editDto, daemonSet);
        daemonSet.getMetadata().setResourceVersion(null);
        daemonSet.getMetadata().setUid(null);
        if (log.isDebugEnabled()) {
            log.debug("daemonSet: " + JSON.toJSONString(daemonSet));
        }
        k8sClientHolder.getClient().apps().daemonSets().inNamespace(namespace).createOrReplace(daemonSet);
        this.patchCache(daemonSet);
    }


    /**
     * 获取http网关
     */
    public List<DaemonSet> queryHttpIngressController() {
        List<DaemonSet> ingressControllers = listByLabelCustom("app.kubernetes.io/name", "ingress", "ingress-tcp");
        if (CollectionUtils.isEmpty(ingressControllers)) {
            ingressControllers = listByLabelCustom("app.type", "ingress-controller");
        }
        List<DaemonSet> httpIngressControllers = ingressControllers.stream().filter(it ->
                StringUtils.equalsIgnoreCase("http", it.getMetadata().getLabels().get("ingress/protocol"))
                        || StringUtils.isEmpty(it.getMetadata().getLabels().get("ingress/protocol"))).collect(Collectors.toList());
        return httpIngressControllers;
    }


    /**
     * 获取http网关
     */
    public List<DaemonSet> queryTcpIngressController() {
        List<DaemonSet> ingressControllers = listByLabelCustom("app.kubernetes.io/name", "ingress", "ingress-tcp");
        if (CollectionUtils.isEmpty(ingressControllers)) {
            ingressControllers = listByLabelCustom("app.type", "ingress-controller");
        }
        List<DaemonSet> ttpIngressControllers = ingressControllers.stream().filter(it ->
                StringUtils.equalsIgnoreCase("tcp", it.getMetadata().getLabels().get("ingress/protocol"))
                        || StringUtils.isEmpty(it.getMetadata().getLabels().get("ingress/protocol"))).collect(Collectors.toList());
        return ttpIngressControllers;
    }

    //------------------------------------------------------------------------以下为重构方法--------------------------------/
    @Override
    public DaemonSet toSimpleData(HasMetadata hasMetadata) {
        DaemonSet daemonSet = (DaemonSet) hasMetadata;
        daemonSet.getMetadata().setResourceVersion(null);
        daemonSet.getMetadata().setSelfLink(null);
        daemonSet.getMetadata().setUid(null);
        daemonSet.getMetadata().setManagedFields(null);

        daemonSet.getSpec().getTemplate().setMetadata(null);
        daemonSet.getSpec().getTemplate().getSpec().setAffinity(null);
        daemonSet.getSpec().getTemplate().getSpec().setVolumes(null);
        List<Container> containers = daemonSet.getSpec().getTemplate().getSpec().getContainers();
        for (Container container : containers) {
            container.setEnv(null);
            container.setCommand(null);
            container.setVolumeMounts(null);
            container.setReadinessProbe(null);
            container.setLivenessProbe(null);
            container.setArgs(null);
        }
        List<Container> initContainers = daemonSet.getSpec().getTemplate().getSpec().getInitContainers();
        for (Container initContainer : initContainers) {
            initContainer.setEnv(null);
            initContainer.setCommand(null);
            initContainer.setVolumeMounts(null);
            initContainer.setReadinessProbe(null);
            initContainer.setLivenessProbe(null);
            initContainer.setArgs(null);
        }
        daemonSet.getStatus().setConditions(null);
        return daemonSet;
    }

    @Override
    public List<DaemonSet> list() {
        log.info("list all");
        return k8sClientHolder.getClient().apps().daemonSets().inAnyNamespace().list().getItems();
    }

    @Override
    public List<DaemonSet> list(String namespace) {
        log.info("get => namespace:{}", namespace);
        if (StringUtils.isBlank(namespace)) {
            return this.list();
        }
        return k8sClientHolder.getClient().apps().daemonSets().inNamespace(namespace).list().getItems();
    }

    public List<DaemonSet> list(Long limit) {
        log.info("get => limit:{}", limit);
        return k8sClientHolder.getClient().apps().daemonSets().inAnyNamespace().list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    public List<DaemonSet> list(String namespace, Long limit) {
        // 空间为空时，查询当前集群下所有信息
        if (StringUtils.isBlank(namespace)) {
            return this.list(limit);
        }
        log.info("get => namespace:{} ,limit:{}", namespace, limit);
        return k8sClientHolder.getClient().apps().daemonSets().inNamespace(namespace).list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    /**
     * 根据名称获取对象
     *
     * @param namespace 必填，否则无法查询name
     * @param name
     * @return
     */
    public DaemonSet get(String name, String namespace) {
        log.info("get => namespace:{} ,name:{}", namespace, name);
        return k8sClientHolder.getClient().apps().daemonSets().inNamespace(namespace).withName(name).get();
    }

    public List<DaemonSet> listByLabels(Map<String, String> labels) {
        log.info("get => labels:{}", labels);
        return k8sClientHolder.getClient().apps().daemonSets().inAnyNamespace().withLabels(labels).list().getItems();
    }

    public List<DaemonSet> listByLabels(String namespace, Map<String, String> labels) {
        log.info("get => namespace:{},labels:{}", namespace, labels);
        if (StringUtils.isEmpty(namespace)) {
            return this.listByLabels(labels);
        }
        return k8sClientHolder.getClient().apps().daemonSets().inNamespace(namespace).withLabels(labels).list().getItems();
    }

    public List<DaemonSet> listByLabels(List<String> labels) {
        log.info("get => labels:{}", labels);
        List<LabelSelectorRequirement> requirements = Lists.newArrayList();
        for (String label : labels) {
            requirements.add(new LabelSelectorRequirementBuilder()
                    .withKey(label).withOperator("Exists").build());
        }
        return k8sClientHolder.getClient().apps().daemonSets().inAnyNamespace().withLabelSelector(new LabelSelectorBuilder().withMatchExpressions(requirements).build()).list().getItems();
    }

    public List<DaemonSet> listByLabels(String namespace, List<String> labels) {
        if (StringUtils.isBlank(namespace)) {
            return this.listByLabels(labels);
        }
        List<LabelSelectorRequirement> requirements = Lists.newArrayList();
        for (String label : labels) {
            requirements.add(new LabelSelectorRequirementBuilder()
                    .withKey(label).withOperator("Exists").build());
        }
        log.info("get => namespace:{},labels:{},requirements", namespace, labels, requirements);
        return k8sClientHolder.getClient().apps().daemonSets().inNamespace(namespace).withLabelSelector(new LabelSelectorBuilder().withMatchExpressions(requirements).build()).list().getItems();
    }

    public List<DaemonSet> listByLabel(String namespace, String label, String... value) {
        if (StringUtils.isBlank(namespace)) {
            return this.listByLabelCustom(label, value);
        }
        log.info("get => namespace:{} ,label:{},value{}", namespace, value);
        return k8sClientHolder.getClient().apps().daemonSets().inNamespace(namespace).withLabelIn(label, value).list().getItems();

    }

    public List<DaemonSet> listByLabelCustom(String label, String... value) {
        log.info("get => label:{},value{}", label, value);
        return k8sClientHolder.getClient().apps().daemonSets().inAnyNamespace().withLabelIn(label, value).list().getItems();
    }
}
