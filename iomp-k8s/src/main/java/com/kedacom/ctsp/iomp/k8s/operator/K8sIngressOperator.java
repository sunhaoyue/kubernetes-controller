package com.kedacom.ctsp.iomp.k8s.operator;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import com.kedacom.ctsp.iomp.k8s.common.cache.K8sAbstractOperator;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressTLS;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class K8sIngressOperator extends K8sAbstractOperator {

    public K8sIngressOperator(K8sClientHolder k8sClientHolder) {
        super(k8sClientHolder);
    }


    public void createOrReplace(Ingress ingress) {
        log.info("update => ingress:{}", ingress);
        if (ingress.getMetadata() != null) {
            k8sClientHolder.getClient().extensions().ingresses().inNamespace(ingress.getMetadata().getNamespace()).createOrReplace(ingress);
            this.patchCache(ingress);
        }

    }

    public void createOrReplace(String namespace, Ingress ingress) {
        log.info("update => ingress:{}", ingress);
        k8sClientHolder.getClient().extensions().ingresses().inNamespace(namespace).createOrReplace(ingress);
        this.patchCache(ingress);
    }


    public boolean createOrReplace(String name, String namespace, String yaml) {
        log.info("update => name:{},nameSpace:{},yaml:{}", name, namespace, yaml);
        try {
            DefaultKubernetesClient client = k8sClientHolder.getClient();
            Ingress ingress = client.extensions().ingresses().inNamespace(namespace).withName(name).get();
            if (ingress != null) {
                Ingress newIngress = new Yaml().loadAs(yaml, Ingress.class);
                newIngress.getMetadata().setResourceVersion(null);
                client.extensions().ingresses().inNamespace(namespace).createOrReplace(newIngress);
                this.patchCache(newIngress);
                if (log.isDebugEnabled()) {
                    log.debug("newIngress: " + JSON.toJSONString(newIngress));
                }
                return true;
            }
        } catch (Exception e) {
            log.error("patch k8sIngress by yaml found exception, k8sIngress:{}, namespace:{}, yaml:{}", name, namespace, yaml, e);
        }
        return false;
    }

    public boolean delete(String name, String namespace) {
        log.info("delete => name:{},nameSpace:{}", name, namespace);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        boolean result = client.extensions().ingresses().inNamespace(namespace).withName(name).delete();
        if (result) {
            this.delCache(name, namespace);
        }
        return result;
    }

    public Boolean deleteWithLabelIn(String namespace, String key, String... values) {
        log.info("delete => namespace:{},key:{},values:{}", key, values);
        boolean result;
        if (StringUtils.isNotBlank(namespace)) {
            result = k8sClientHolder.getClient().extensions().ingresses().inNamespace(namespace).withLabelIn(key, values).delete();
        } else {
            result = k8sClientHolder.getClient().extensions().ingresses().withLabelIn(key, values).delete();
        }
        if (result) {
            this.delCacheByLabel(namespace, key, values);
        }
        return result;
    }


    public List<Ingress> getByIngressClass(String namespace, String ingressClass) {
        log.info("get => namespace:{},ingressClass", namespace, ingressClass);
        List<Ingress> ingressList = Lists.newArrayList();
        if (StringUtils.isNotEmpty(namespace)) {
            ingressList = k8sClientHolder.getClient().extensions().ingresses().inNamespace(namespace).list().getItems();
        } else {
            ingressList = k8sClientHolder.getClient().extensions().ingresses().inAnyNamespace().list().getItems();
        }
        if (CollectionUtils.isEmpty(ingressList)) {
            return Lists.newArrayList();
        }
        Iterator<Ingress> iterator = ingressList.iterator();
        while (iterator.hasNext()) {
            Ingress ingress = iterator.next();
            if (!StringUtils.equals(ingress.getSpec().getIngressClassName(), ingressClass)) {
                iterator.remove();
            }
        }
        return ingressList;
    }


    public List<Ingress> getByAnnotation(String namespace, String key, String... values) {
        log.info("get => namespace:{},key:{},value:{}", namespace, key, values);
        List<Ingress> ingressList = Lists.newArrayList();
        if (StringUtils.isNotEmpty(namespace)) {
            ingressList = k8sClientHolder.getClient().extensions().ingresses().inNamespace(namespace).list().getItems();
        } else {
            ingressList = k8sClientHolder.getClient().extensions().ingresses().inAnyNamespace().list().getItems();
        }
        if (CollectionUtils.isEmpty(ingressList)) {
            return Lists.newArrayList();
        }
        List<String> valueList = Arrays.asList(values);
        Iterator<Ingress> iterator = ingressList.iterator();
        while (iterator.hasNext()) {
            Ingress ingress = iterator.next();
            if (ingress.getMetadata().getAnnotations() != null) {
                String ingressClass = ingress.getMetadata().getAnnotations().get(key);
                if (!valueList.contains(ingressClass)) {
                    iterator.remove();
                }
            }
        }
        return ingressList;
    }

    public List<Ingress> getBySpecRule(String namespace, String key, String certName) {
        log.info("get => namespace:{},key:{},certName:{}", namespace, key, certName);
        List<Ingress> ingressList = new ArrayList<>();
        if (StringUtils.isBlank(namespace)) {
            ingressList = k8sClientHolder.getClient().extensions().ingresses().inNamespace(namespace).list().getItems();
        } else {
            ingressList = k8sClientHolder.getClient().extensions().ingresses().inAnyNamespace().list().getItems();
        }
        if (CollectionUtils.isEmpty(ingressList)) {
            return Lists.newArrayList();
        }
        Iterator<Ingress> iterator = ingressList.iterator();
        List<Ingress> result = new ArrayList<>();
        while (iterator.hasNext()) {
            Ingress ingress = iterator.next();
            if (ingress.getSpec() != null && !CollectionUtils.isEmpty(ingress.getSpec().getTls())) {
                List<IngressTLS> rules = ingress.getSpec().getTls();
                switch (key) {
                    case "host":
                        IngressTLS tls = rules.stream().filter(t -> !StringUtils.isBlank(t.getSecretName()) && t.getSecretName().equals(certName)).findFirst().orElse(null);
                        if (tls != null) {
                            result.add(ingress);
                        }
                        break;
                }
            }
        }
        return result;
    }

    //------------------------------------------------------------------------以下为重构方法--------------------------------/
    @Override
    public Ingress toSimpleData(HasMetadata hasMetadata) {
        Ingress ingress = (Ingress) hasMetadata;
        ingress.getMetadata().setResourceVersion(null);
        ingress.getMetadata().setSelfLink(null);
        ingress.getMetadata().setUid(null);
        ingress.getMetadata().setManagedFields(null);
        ingress.getMetadata().setOwnerReferences(null);

        return ingress;
    }

    @Override
    public List<Ingress> list() {
        log.info("get => list all");
        return k8sClientHolder.getClient().extensions().ingresses().inAnyNamespace().list().getItems();
    }

    @Override
    public List<Ingress> list(String namespace) {

        if (StringUtils.isBlank(namespace)) {
            return this.list();
        }
        log.info("get => namespace:{}", namespace);
        return k8sClientHolder.getClient().extensions().ingresses().inNamespace(namespace).list().getItems();
    }

    public List<Ingress> list(Long limit) {
        log.info("get => limit:{}", limit);
        return k8sClientHolder.getClient().extensions().ingresses().inAnyNamespace().list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    public List<Ingress> list(String namespace, Long limit) {

        // 若值为空或小于0则默认100条
        if (limit == null || limit < 0) {
            return this.list(namespace);
        }

        // 空间为空时，查询当前集群下所有信息
        if (StringUtils.isBlank(namespace)) {
            return this.list(limit);
        }
        log.info("get => namespace:{} ,limit:{}", namespace, limit);
        return k8sClientHolder.getClient().extensions().ingresses().inNamespace(namespace).list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    /**
     * 根据名称获取对象
     *
     * @param namespace 必填，否则无法查询name
     * @param name
     * @return
     */
    public Ingress get(String name, String namespace) {
        log.info("namespace:{} ,name:{}", namespace, name);
        return k8sClientHolder.getClient().extensions().ingresses().inNamespace(namespace).withName(name).get();
    }


    public List<Ingress> listByLabel(String namespace, String label, String... value) {
        if (StringUtils.isBlank(namespace)) {
            return this.listByLabelCustom(label, value);
        }
        log.info("get => namespace:{} ,label:{},value{}", namespace, value);
        return k8sClientHolder.getClient().extensions().ingresses().inNamespace(namespace).withLabelIn(label, value).list().getItems();

    }

    public List<Ingress> listByLabelCustom(String label, String... value) {
        log.info("get => label:{},value{}", label, value);
        return k8sClientHolder.getClient().extensions().ingresses().inAnyNamespace().withLabelIn(label, value).list().getItems();
    }

}
