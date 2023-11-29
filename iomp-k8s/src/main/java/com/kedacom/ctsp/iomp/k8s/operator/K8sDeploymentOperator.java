package com.kedacom.ctsp.iomp.k8s.operator;

import com.alibaba.fastjson.JSON;
import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import com.kedacom.ctsp.iomp.k8s.common.cache.K8sAbstractOperator;
import com.kedacom.ctsp.iomp.k8s.constant.K8sConstants;
import com.kedacom.ctsp.iomp.k8s.convert.DeploymentConvert;
import com.kedacom.ctsp.iomp.k8s.dto.DeploymentEditDto;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class K8sDeploymentOperator extends K8sAbstractOperator {

    public K8sDeploymentOperator(K8sClientHolder k8sClientHolder) {
        super(k8sClientHolder);
    }

    public boolean scale(String name, String namespace, int replicas) {
        log.info("update => name:{},namespace:{},replicas:{}", name, namespace, replicas);
        try {
            DefaultKubernetesClient client = k8sClientHolder.getClient();
            Deployment deployment = client.apps().deployments().inNamespace(namespace).withName(name).get();
            if (deployment != null) {
                deployment.getMetadata().setResourceVersion(null);
                deployment.getSpec().setReplicas(replicas);
                if (log.isDebugEnabled()) {
                    log.debug("deployment: " + JSON.toJSONString(deployment));
                }
                client.apps().deployments().inNamespace(namespace).createOrReplace(deployment);
                this.patchCache(deployment);
                return true;
            }
        } catch (Exception e) {
            log.error("scale deployment found exception, deployment:{},namespace:{}", name, namespace, e);
        }
        return false;
    }

    public void createOrReplace(Deployment deployment) {
        log.info("update => deployment:{}", JSON.toJSONString(deployment));
        k8sClientHolder.getClient().apps().deployments().inNamespace(deployment.getMetadata().getNamespace()).createOrReplace(deployment);
        // 更新缓存
        this.patchCache(deployment);
    }

    public void createOrReplace(String namespace, Deployment deployment) {
        log.info("update => namespace:{},deployment:{}", namespace, JSON.toJSONString(deployment));
        k8sClientHolder.getClient().apps().deployments().inNamespace(namespace).createOrReplace(deployment);
        // 更新缓存
        this.patchCache(deployment);
    }

    public boolean createOrReplace(String name, String namespace, String yaml) {
        log.info("update => name:{},namespace:{},data:{},yaml:{}", name, namespace, yaml);
        try {
            DefaultKubernetesClient client = k8sClientHolder.getClient();
            Deployment deployment = client.apps().deployments().inNamespace(namespace).withName(name).get();
            if (deployment != null) {
                Deployment newDeployment = new Yaml().loadAs(yaml, Deployment.class);
                newDeployment.getMetadata().setResourceVersion(null);
                newDeployment.setApiVersion(K8sConstants.API_VERSION_DEPLOYMENT);
                client.apps().deployments().inNamespace(namespace).createOrReplace(newDeployment);
                this.patchCache(newDeployment);
                if (log.isDebugEnabled()) {
                    log.debug("deployment: " + JSON.toJSONString(deployment));
                }
                return true;
            }
        } catch (Exception e) {
            log.error("patch deployment by yaml found exception, deployment:{}, namespace:{}, yaml:{}", name, namespace, yaml, e);
        }
        return false;
    }

    public boolean delete(String name, String namespace) {
        log.info("delete => name:{},namespace:{}", name, namespace);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        boolean result = client.apps().deployments().inNamespace(namespace).withName(name).delete();
        if (result) {
            this.delCache(name, namespace);
        }
        return result;
    }

    public Boolean deleteWithLabelIn(String namespace, String key, String... values) {
        log.info("delete => namespace:{},key:{},values:{}", key, values);
        boolean result;
        if (StringUtils.isNotBlank(namespace)) {
            result = k8sClientHolder.getClient().apps().deployments().inNamespace(namespace).withLabelIn(key, values).delete();
        } else {
            result = k8sClientHolder.getClient().apps().deployments().withLabelIn(key, values).delete();
        }
        if (result) {
            this.delCacheByLabel(namespace, key, values);
        }
        return result;
    }


    public void patch(String name, String namespace, DeploymentEditDto deploymentEditDto) {
        log.info("update => name:{},namespace:{},deploymentEditDto:{}", name, namespace, JSON.toJSONString(deploymentEditDto));
        Deployment deployment = get(name, namespace);
        deployment = new DeploymentConvert().merge(deploymentEditDto, deployment);
        deployment.getMetadata().setResourceVersion(null);
        deployment.getMetadata().setUid(null);
        if (log.isDebugEnabled()) {
            log.debug("deployment: " + JSON.toJSONString(deployment));
        }
        k8sClientHolder.getClient().apps().deployments().inNamespace(namespace).createOrReplace(deployment);
        // 更新缓存
        this.patchCache(deployment);
    }


    //------------------------------------------------------------------------以下为重构方法--------------------------------/
    @Override
    public Deployment toSimpleData(HasMetadata hasMetadata) {
        Deployment deployment = (Deployment) hasMetadata;
        deployment.getMetadata().setResourceVersion(null);
        deployment.getMetadata().setSelfLink(null);
        deployment.getMetadata().setUid(null);
        deployment.getMetadata().setManagedFields(null);

        deployment.getSpec().getTemplate().setMetadata(null);
        deployment.getSpec().getTemplate().getSpec().setAffinity(null);
        deployment.getSpec().getTemplate().getSpec().setVolumes(null);
        List<Container> containers = deployment.getSpec().getTemplate().getSpec().getContainers();
        for (Container container : containers) {
            container.setEnv(null);
            container.setCommand(null);
            container.setVolumeMounts(null);
            container.setReadinessProbe(null);
            container.setLivenessProbe(null);
            container.setArgs(null);
        }
        List<Container> initContainers = deployment.getSpec().getTemplate().getSpec().getInitContainers();
        for (Container initContainer : initContainers) {
            initContainer.setEnv(null);
            initContainer.setCommand(null);
            initContainer.setVolumeMounts(null);
            initContainer.setReadinessProbe(null);
            initContainer.setLivenessProbe(null);
            initContainer.setArgs(null);
        }
        deployment.getStatus().setConditions(null);
        return deployment;
    }


    @Override
    public List<Deployment> list() {
        log.info("list all");
        return k8sClientHolder.getClient().apps().deployments().inAnyNamespace().list().getItems();
    }

    @Override
    public List<Deployment> list(String namespace) {

        if (StringUtils.isBlank(namespace)) {
            return this.list();
        }
        log.info("get => namespace:{}", namespace);
        return k8sClientHolder.getClient().apps().deployments().inNamespace(namespace).list().getItems();
    }

    public List<Deployment> list(Long limit) {
        log.info("get => limit:{}", limit);
        return k8sClientHolder.getClient().apps().deployments().inAnyNamespace().list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    public List<Deployment> list(String namespace, Long limit) {

        // 若值为空或小于0则默认100条
        if (limit == null || limit < 0) {
            return this.list(namespace);
        }

        // 空间为空时，查询当前集群下所有信息
        if (StringUtils.isBlank(namespace)) {
            return this.list(limit);
        }
        log.info("get => namespace:{} ,limit:{}", namespace, limit);
        return k8sClientHolder.getClient().apps().deployments().inNamespace(namespace).list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    /**
     * 以前方法只会返回一个（有歧义）
     *
     * @param name
     * @return
     */
    // TODO: 2021/12/26 有歧义
    public Deployment get(String name) {
        log.info("get => name:{}", name);
        List<Deployment> result = new ArrayList<>();
        List<Namespace> items = k8sClientHolder.getClient().namespaces().list().getItems();
        if (CollectionUtils.isEmpty(items)) {
            return new Deployment();
        }

        for (Namespace item : items) {
            Deployment deployment = get(name, item.getMetadata().getName());
            if (deployment != null) {
                result.add(deployment);
            }
        }
        if (CollectionUtils.isEmpty(result)) {
            return null;
        }
        return result.get(0);
    }

    /**
     * 根据名称获取对象
     *
     * @param namespace 必填，否则无法查询name
     * @param name
     * @return
     */
    public Deployment get(String name, String namespace) {
        log.info("get => namespace:{} ,name:{}", namespace, name);
        return k8sClientHolder.getClient().apps().deployments().inNamespace(namespace).withName(name).get();
    }

    public List<Deployment> listByLabels(Map<String, String> labels) {
        log.info("get => labels:{}", labels);
        return k8sClientHolder.getClient().apps().deployments().inAnyNamespace().withLabels(labels).list().getItems();
    }


    public List<Deployment> listByLabels(String namespace, Map<String, String> labels) {
        log.info("get => namespace:{},labels:{}", namespace, labels);
        if (StringUtils.isEmpty(namespace)) {
            return this.listByLabels(labels);
        }
        return k8sClientHolder.getClient().apps().deployments().inNamespace(namespace).withLabels(labels).list().getItems();
    }

}
