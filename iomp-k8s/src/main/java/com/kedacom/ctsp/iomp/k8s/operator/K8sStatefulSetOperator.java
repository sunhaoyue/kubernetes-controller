package com.kedacom.ctsp.iomp.k8s.operator;

import com.alibaba.fastjson.JSON;
import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import com.kedacom.ctsp.iomp.k8s.common.cache.K8sAbstractOperator;
import com.kedacom.ctsp.iomp.k8s.constant.K8sConstants;
import com.kedacom.ctsp.iomp.k8s.convert.StatefulSetConvert;
import com.kedacom.ctsp.iomp.k8s.dto.DeploymentEditDto;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class K8sStatefulSetOperator extends K8sAbstractOperator {

    public K8sStatefulSetOperator(K8sClientHolder k8sClientHolder) {
        super(k8sClientHolder);
    }

    public boolean scale(String name, String namespace, int replicas) {
        log.info("update => name:{},namespace:{},replicas:{}", name, namespace, replicas);
        try {
            DefaultKubernetesClient client = k8sClientHolder.getClient();
            StatefulSet statefulSet = client.apps().statefulSets().inNamespace(namespace).withName(name).get();
            if (statefulSet != null) {
                statefulSet.getMetadata().setResourceVersion(null);
                statefulSet.getSpec().setReplicas(replicas);
                if (log.isDebugEnabled()) {
                    log.debug("statefulSet:{}", JSON.toJSONString(statefulSet));
                }
                client.apps().statefulSets().inNamespace(namespace).createOrReplace(statefulSet);
                this.patchCache(statefulSet);
                return true;
            }
        } catch (Exception e) {
            log.error("scale statefulset found exception, statefulset:{},namespace:{}", name, namespace, e);
        }
        return false;
    }

    public boolean createOrReplace(String name, String namespace, String yaml) {
        log.info("update => name:{},namespace:{},yaml:{}", name, namespace, yaml);
        try {
            DefaultKubernetesClient client = k8sClientHolder.getClient();
            StatefulSet statefulSet = client.apps().statefulSets().inNamespace(namespace).withName(name).get();
            if (statefulSet != null) {
                StatefulSet newStatefulSet = new Yaml().loadAs(yaml, StatefulSet.class);
                newStatefulSet.getMetadata().getLabels().put("app.kubernetes.io/managed-by", "iomp-api");
                newStatefulSet.getMetadata().setResourceVersion(null);
                newStatefulSet.setApiVersion(K8sConstants.API_VERSION_DEPLOYMENT);
                if (log.isDebugEnabled()) {
                    log.info("newStatefulSet:{}", JSON.toJSONString(newStatefulSet));
                }
                client.apps().statefulSets().inNamespace(namespace).createOrReplace(newStatefulSet);
                this.patchCache(newStatefulSet);
                return true;
            }
        } catch (Exception e) {
            log.error("patch statefulset by yaml found exception, statefulset:{}, namespace:{}, yaml:{}", name, namespace, yaml, e);
        }
        return false;
    }

    public boolean delete(String name, String namespace) {
        log.info("delete => name:{},namespace:{}", name, namespace);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        boolean result = client.apps().statefulSets().inNamespace(namespace).withName(name).delete();
        if (result) {
            this.delCache(name,namespace);
        }
        return result;
    }


    public Boolean deleteWithLabelIn(String namespace,String key, String... values) {
        log.info("delete => namespace:{},key:{},values:{}", key, values);
        boolean result;
        if(StringUtils.isNotBlank(namespace)){
            result = k8sClientHolder.getClient().apps().statefulSets().inNamespace(namespace).withLabelIn(key, values).delete();
        }else{
            result = k8sClientHolder.getClient().apps().statefulSets().withLabelIn(key, values).delete();
        }
        if(result){
            this.delCacheByLabel(namespace,key,values);
        }
        return result;
    }



    public void patch(String deployName, String namespace, DeploymentEditDto editDto) {
        log.info("update => deployName:{},namespace:{},editDto:{}", deployName, namespace, JSON.toJSONString(editDto));
        StatefulSet statefulSet = get(deployName, namespace);
        statefulSet = new StatefulSetConvert().merge(editDto, statefulSet);
        statefulSet.getMetadata().setResourceVersion(null);
        statefulSet.getMetadata().setUid(null);
        if (log.isDebugEnabled()) {
            log.debug("statefulSet:{}", JSON.toJSONString(statefulSet));
        }
        k8sClientHolder.getClient().apps().statefulSets().inNamespace(namespace).createOrReplace(statefulSet);
        this.patchCache(statefulSet);
    }


    public void createOrReplace(String namespace,StatefulSet statefulSet) {
        log.info("update => statefulSet:{}", JSON.toJSONString(statefulSet));
        k8sClientHolder.getClient().apps().statefulSets().inNamespace(namespace).createOrReplace(statefulSet);
        this.patchCache(statefulSet);
    }

    public void createOrReplace(StatefulSet statefulSet) {
        log.info("update => statefulSet:{}", JSON.toJSONString(statefulSet));
        k8sClientHolder.getClient().apps().statefulSets().inNamespace(statefulSet.getMetadata().getNamespace()).createOrReplace(statefulSet);
        this.patchCache(statefulSet);
    }
    //------------------------------------------------------------------------以下为重构方法--------------------------------/
    @Override
    public StatefulSet toSimpleData(HasMetadata hasMetadata) {
        StatefulSet statefulSet = (StatefulSet) hasMetadata;
        statefulSet.getMetadata().setResourceVersion(null);
        statefulSet.getMetadata().setSelfLink(null);
        statefulSet.getMetadata().setUid(null);
        statefulSet.getMetadata().setManagedFields(null);

        statefulSet.getSpec().getTemplate().setMetadata(null);
        statefulSet.getSpec().getTemplate().getSpec().setAffinity(null);
        statefulSet.getSpec().getTemplate().getSpec().setVolumes(null);
        List<Container> containers = statefulSet.getSpec().getTemplate().getSpec().getContainers();
        for (Container container : containers) {
            container.setEnv(null);
            container.setCommand(null);
            container.setVolumeMounts(null);
            container.setReadinessProbe(null);
            container.setLivenessProbe(null);
            container.setArgs(null);
        }
        List<Container> initContainers = statefulSet.getSpec().getTemplate().getSpec().getInitContainers();
        for (Container initContainer : initContainers) {
            initContainer.setEnv(null);
            initContainer.setCommand(null);
            initContainer.setVolumeMounts(null);
            initContainer.setReadinessProbe(null);
            initContainer.setLivenessProbe(null);
            initContainer.setArgs(null);
        }
        statefulSet.getStatus().setConditions(null);
        return statefulSet;
    }

    public  List<StatefulSet> listByLabels(Map<String, String> labels){
        log.info("get => labels:{}", labels);
        return k8sClientHolder.getClient().apps().statefulSets().inAnyNamespace().withLabels(labels).list().getItems();
    }


    public List<StatefulSet> listByLabels(String namespace, Map<String, String> labels){
        log.info("get => namespace:{},labels:{}", namespace, labels);
        if (StringUtils.isEmpty(namespace)) {
            return this.listByLabels(labels);
        }
        return k8sClientHolder.getClient().apps().statefulSets().inNamespace(namespace).withLabels(labels).list().getItems();
    }



    public List<StatefulSet> list() {
        log.info("get => list all");
        return k8sClientHolder.getClient().apps().statefulSets().inAnyNamespace().list().getItems();
    }

    public List<StatefulSet> list(String namespace) {

        if (StringUtils.isBlank(namespace)) {
            return this.list();
        }
        log.info("get => namespace:{}", namespace);
        return k8sClientHolder.getClient().apps().statefulSets().inNamespace(namespace).list().getItems();
    }

    public List<StatefulSet> list(Long limit) {
        log.info("get => limit:{}", limit);
        return k8sClientHolder.getClient().apps().statefulSets().inAnyNamespace().list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    public List<StatefulSet> list(String namespace, Long limit) {

        // 若值为空或小于0则默认100条
        if (limit == null || limit < 0) {
            return this.list(namespace);
        }

        // 空间为空时，查询当前集群下所有信息
        if (StringUtils.isBlank(namespace)) {
            return this.list(limit);
        }
        log.info("get => namespace:{} ,limit:{}", namespace, limit);
        return k8sClientHolder.getClient().apps().statefulSets().inNamespace(namespace).list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    public List<StatefulSet> get(String name) {
        log.info("get => name:{}", name);
        List<StatefulSet> result = new ArrayList<>();
        List<Namespace> items = k8sClientHolder.getClient().namespaces().list().getItems();
        if (CollectionUtils.isEmpty(items)) {
            return result;
        }

        for (Namespace item : items) {
            StatefulSet configMap = get(name, item.getMetadata().getName());
            if (configMap != null) {
                result.add(configMap);
            }
        }
        return result;
    }

    /**
     * 根据名称获取对象
     *
     * @param namespace 必填，否则无法查询name
     * @param name
     * @return
     */
    public StatefulSet get(String name, String namespace) {
        log.info("get => name:{} ,namespace:{}", name, namespace);
        return k8sClientHolder.getClient().apps().statefulSets().inNamespace(namespace).withName(name).get();
    }

}
