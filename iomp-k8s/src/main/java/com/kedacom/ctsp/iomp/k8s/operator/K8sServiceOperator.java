package com.kedacom.ctsp.iomp.k8s.operator;

import com.alibaba.fastjson.JSON;
import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import com.kedacom.ctsp.iomp.k8s.common.cache.K8sAbstractOperator;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.List;

@Slf4j
public class K8sServiceOperator extends K8sAbstractOperator {

    public K8sServiceOperator(K8sClientHolder k8sClientHolder) {
        super(k8sClientHolder);
    }

    public void createOrReplace(Service service) {
        log.info("update => service:{}", JSON.toJSONString(service));
        if (service.getMetadata() != null) {
            k8sClientHolder.getClient().services().inNamespace(service.getMetadata().getNamespace()).createOrReplace(service);
            this.patchCache(service);
        }
    }

    public void createOrReplace(String namespace, Service service) {
        log.info("update => namespace:{},service:{}", namespace, JSON.toJSONString(service));
        k8sClientHolder.getClient().services().inNamespace(namespace).createOrReplace(service);
        this.patchCache(service);
    }


    public boolean createOrReplace(String name, String namespace, String yaml) {
        try {
            log.info("update => name:{},namespace:{},yaml:{}", name, namespace, yaml);
            DefaultKubernetesClient client = k8sClientHolder.getClient();
            Service service = client.services().inNamespace(namespace).withName(name).get();
            if (service != null) {
                Service newService = new Yaml().loadAs(yaml, Service.class);
                newService.getMetadata().setResourceVersion(null);
                if (log.isDebugEnabled()) {
                    log.debug("service:{}", JSON.toJSONString(newService));
                }
                client.services().inNamespace(namespace).createOrReplace(newService);
                this.patchCache(newService);
                return true;
            }
        } catch (Exception e) {
            log.error("patch k8sService by yaml found exception, k8sService:{}, namespace:{}, yaml:{}", name, namespace, yaml, e);
        }
        return false;
    }

    public boolean delete(String name, String namespace) {
        log.info("delete => name:{},namespace:{}", name, namespace);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        boolean result = client.services().inNamespace(namespace).withName(name).delete();
        if (result) {
            this.delCache(name, namespace);
        }
        return result;
    }

    public Boolean deleteWithLabelIn(String namespace, String key, String... values) {
        log.info("delete => namespace:{},key:{},values:{}", key, values);
        boolean result;
        if (StringUtils.isNotBlank(namespace)) {
            result = k8sClientHolder.getClient().services().inNamespace(namespace).withLabelIn(key, values).delete();
        } else {
            result = k8sClientHolder.getClient().services().withLabelIn(key, values).delete();
        }
        if (result) {
            this.delCacheByLabel(namespace, key, values);
        }
        return result;
    }


    /**
     * endpoints的名称与service的名称一样
     *
     * @param serviceName
     * @param namespace
     * @return
     */
    public Endpoints getServiceEndpoints(String serviceName, String namespace) {
        log.info("get => serviceName:{},namespace:{}", serviceName, namespace);
        return k8sClientHolder.getClient().endpoints().inNamespace(namespace).withName(serviceName).get();
    }

    //------------------------------------------------------------------------以下为重构方法--------------------------------/
    @Override
    public Service toSimpleData(HasMetadata hasMetadata) {
        Service service = (Service) hasMetadata;
        service.getMetadata().setResourceVersion(null);
        service.getMetadata().setSelfLink(null);
        service.getMetadata().setUid(null);
        service.getMetadata().setManagedFields(null);
        service.getMetadata().setOwnerReferences(null);

        return service;
    }

    @Override
    public List<Service> list() {
        log.info("get => list all");
        return k8sClientHolder.getClient().services().inAnyNamespace().list().getItems();
    }

    @Override
    public List<Service> list(String namespace) {

        if (StringUtils.isBlank(namespace)) {
            return this.list();
        }
        log.info("get => namespace:{}", namespace);
        return k8sClientHolder.getClient().services().inNamespace(namespace).list().getItems();
    }

    public List<Service> list(Long limit) {
        log.info("get => limit:{}", limit);
        return k8sClientHolder.getClient().services().inAnyNamespace().list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    public List<Service> list(String namespace, Long limit) {

        // 若值为空或小于0则默认100条
        if (limit == null || limit < 0) {
            return this.list(namespace);
        }

        // 空间为空时，查询当前集群下所有信息
        if (StringUtils.isBlank(namespace)) {
            return this.list(limit);
        }
        log.info("get => namespace:{} ,limit:{}", namespace, limit);
        return k8sClientHolder.getClient().services().inNamespace(namespace).list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    /**
     * 根据名称获取对象
     *
     * @param namespace 必填，否则无法查询name
     * @param name
     * @return
     */
    public Service get(String name, String namespace) {
        log.info("get => name:{} ,namespace:{}", name, namespace);
        return k8sClientHolder.getClient().services().inNamespace(namespace).withName(name).get();
    }

    public List<Service> listByLabel(String namespace, String label, String... value) {
        if (StringUtils.isBlank(namespace)) {
            return this.listByLabelCustom(label, value);
        }
        log.info("get => namespace:{} ,label:{},value{}", namespace, value);
        return k8sClientHolder.getClient().services().inNamespace(namespace).withLabelIn(label, value).list().getItems();

    }

    public List<Service> listByLabelCustom(String label, String... value) {
        log.info("get => label:{},value{}", label, value);
        return k8sClientHolder.getClient().services().inAnyNamespace().withLabelIn(label, value).list().getItems();
    }

}
