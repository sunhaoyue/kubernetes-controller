package com.kedacom.ctsp.iomp.k8s.operator;


import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import com.kedacom.ctsp.iomp.k8s.common.cache.K8sAbstractOperator;
import com.kedacom.ctsp.iomp.k8s.vo.K8sConfigMapVo;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 */
@Slf4j
public class K8sConfigmapOperator extends K8sAbstractOperator {
    public static final String TCP_LOCK_CM = "tcp-lock";
    public static final String UDP_LOCK_CM = "udp-lock";
    public static final String LOCK_DATA = "lock";
    public static final String LOCK_TIMESTAMP = "timestamp";
    public static final String DOL_INGRESS_TCP_SERVICES = "dol-ingress-tcp-services";
    public static final String DOL_INGRESS_UDP_SERVICES = "dol-ingress-udp-services";
    public static final String LOCK_FLAG = "1";
    public static final String UNLOCK_FLAG = "0";

    public K8sConfigmapOperator(K8sClientHolder k8sClientHolder) {
        super(k8sClientHolder);
    }

    private Map<String, String> getAnnotations(Map<String, String> annotations) {
        log.info("get => annotations:{}", annotations);
        if (annotations == null) {
            annotations = new HashMap<>();
        }
        annotations.put("publisher", "iomp");
        annotations.put("dolphin/managed-time", System.currentTimeMillis() + "");
        return annotations;
    }

    public void createOrReplaceConfigmap(String name, String namespace, Map<String, String> data, boolean updateIfNull) {
        log.info("update => name:{},namespace:{},data:{},updateIfNull:{}", name, namespace, data, updateIfNull);
        if (data == null) {
            return;
        }
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        ConfigMap configMap = client.configMaps().inNamespace(namespace).withName(name).get();

        if (configMap == null) {
            configMap = new ConfigMapBuilder().withNewMetadata()
                    .withAnnotations(getAnnotations(null))
                    .withNamespace(namespace)
                    .withName(name)
                    .endMetadata().build();
            configMap.setData(data);
            if (log.isDebugEnabled()) {
                log.debug("configMap: " + JSON.toJSONString(configMap));
            }
            client.configMaps().inNamespace(namespace).createOrReplace(configMap);
            // 更新缓存
            this.patchCache(configMap);
            return;
        }
        Map<String, String> originDataMap = configMap.getData();
        if (originDataMap == null) {
            originDataMap = Maps.newHashMap();
        }

        for (Map.Entry<String, String> kv : data.entrySet()) {
            if (updateIfNull && originDataMap.get(kv.getKey()) != null) {
                continue;
            }
            originDataMap.put(kv.getKey(), kv.getValue());
        }
        configMap.setData(originDataMap);
        configMap.getMetadata().setAnnotations(getAnnotations(configMap.getMetadata().getAnnotations()));
        if (log.isDebugEnabled()) {
            log.debug("configMap: " + JSON.toJSONString(configMap));
        }
        client.configMaps().inNamespace(namespace).createOrReplace(configMap);
        // 更新缓存
        this.patchCache(configMap);
    }


    public void overwriteConfigmap(String name, String namespace, Map<String, String> data) {
        log.info("update => name:{},namespace:{},data:{},updateIfNull", name, namespace, data);
        if (data == null) {
            return;
        }
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        ConfigMap configMap = client.configMaps().inNamespace(namespace).withName(name).get();

        if (configMap == null) {
            configMap = new ConfigMapBuilder().withNewMetadata()
                    .withAnnotations(getAnnotations(null))
                    .withNamespace(namespace)
                    .withName(name)
                    .endMetadata().build();
            configMap.setData(data);
            if (log.isDebugEnabled()) {
                log.debug("configMap: " + JSON.toJSONString(configMap));
            }
            client.configMaps().inNamespace(namespace).createOrReplace(configMap);
            // 更新缓存
            this.patchCache(configMap);
            return;
        }

        configMap.setData(data);
        configMap.getMetadata().setAnnotations(getAnnotations(configMap.getMetadata().getAnnotations()));
        if (log.isDebugEnabled()) {
            log.debug("configMap: " + JSON.toJSONString(configMap));
        }
        client.configMaps().inNamespace(namespace).createOrReplace(configMap);
        // 更新缓存
        this.patchCache(configMap);
    }

    public void delConfigmap(String name, String namespace, Map<String, String> data, boolean updateIfNull) {
        log.info("update => name:{},namespace:{},data:{},updateIfNull:{}", name, namespace, data, updateIfNull);
        if (data == null) {
            return;
        }
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        ConfigMap configMap = client.configMaps().inNamespace(namespace).withName(name).get();

        if (configMap == null) {
            return;
        }
        Map<String, String> originDataMap = configMap.getData();
        if (originDataMap == null) {
            originDataMap = Maps.newHashMap();
        }

        for (Map.Entry<String, String> kv : data.entrySet()) {
            if (updateIfNull && originDataMap.get(kv.getKey()) != null) {
                continue;
            }
            originDataMap.remove(kv.getKey());
        }
        configMap.setData(originDataMap);
        configMap.getMetadata().setAnnotations(getAnnotations(configMap.getMetadata().getAnnotations()));
        if (log.isDebugEnabled()) {
            log.info("configMap: " + JSON.toJSONString(configMap));
        }
        client.configMaps().inNamespace(namespace).createOrReplace(configMap);
    }


    public void updateConfigMap(K8sConfigMapVo vo) {
        log.info("update => K8sConfigMapVo: " + JSON.toJSONString(vo));
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        ConfigMap configMap = client.configMaps().inNamespace(vo.getNamespace()).withName(vo.getName()).get();
        if (configMap == null) {
            configMap = new ConfigMapBuilder().withNewMetadata()
                    .withNamespace(vo.getNamespace())
                    .withName(vo.getName())
                    .withLabels(vo.getLabels())
                    .withAnnotations(getAnnotations(vo.getAnnotations()))
                    .endMetadata().build();
        }
        configMap.setData(vo.getData());
        configMap.getMetadata().setAnnotations(getAnnotations(vo.getAnnotations()));
        if (log.isDebugEnabled()) {
            log.debug("configMap: " + JSON.toJSONString(configMap));
        }
        client.configMaps().inNamespace(vo.getNamespace()).createOrReplace(configMap);
        // 更新缓存
        this.patchCache(configMap);
    }

    public void addConfigMap(K8sConfigMapVo vo) {
        updateConfigMap(vo);
    }

    /**
     * 空间列表专用
     *
     * @param name
     * @param namespace
     * @param data
     */
    public void addOrUpdateConfigmap(String name, String namespace, Map<String, String> data) {
        log.info("update => name:{},namespace:{},data:{}", name, namespace, data);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        ConfigMap configMap = client.configMaps().inNamespace(namespace).withName(name).get();
        if (configMap == null) {
            configMap = new ConfigMapBuilder().withNewMetadata()
                    .withAnnotations(getAnnotations(null))
                    .withNamespace(namespace)
                    .withName(name)
                    .endMetadata().build();
        }
        configMap.setData(data);
        configMap.getMetadata().setAnnotations(getAnnotations(configMap.getMetadata().getAnnotations()));
        Long startTime = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("configMap: " + JSON.toJSONString(configMap));
        }
        client.configMaps().inNamespace(namespace).createOrReplace(configMap);
        // 更新缓存
        this.patchCache(configMap);
    }

    public void createOrReplaceConfigmap(String name, String namespace, Map<String, String> data) {
        createOrReplaceConfigmap(name, namespace, data, false);
    }

    public void createOrReplaceData(String name, String namespace, Map<String, String> data) {
        if (data == null) {
            return;
        }
        log.info("update => name:{},namespace:{},data:{}", name, namespace, data);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        ConfigMap configMap = client.configMaps().inNamespace(namespace).withName(name).get();
        if (configMap == null) {
            configMap = new ConfigMapBuilder().withNewMetadata()
                    .withAnnotations(getAnnotations(null))
                    .withNamespace(namespace)
                    .withName(name)
                    .endMetadata().build();
            configMap.setData(data);
            client.configMaps().inNamespace(namespace).createOrReplace(configMap);
            // 更新缓存
            this.patchCache(configMap);
            return;
        }
        configMap.setData(data);
        configMap.getMetadata().setAnnotations(getAnnotations(configMap.getMetadata().getAnnotations()));

        if (log.isDebugEnabled()) {
            log.debug("configMap: " + JSON.toJSONString(configMap));
        }
        client.configMaps().inNamespace(namespace).createOrReplace(configMap);
        // 更新缓存
        this.patchCache(configMap);
    }

    public String getValueByKey(String configmap, String namespace, String key) {
        log.info("get => configmap:{},namespace:{},key:{}", configmap, namespace, key);
        ConfigMap configMap = k8sClientHolder.getClient().configMaps().inNamespace(namespace).withName(configmap).get();
        if (configMap != null && configMap.getData() != null) {
            return configMap.getData().get(key);
        }
        return StringUtils.EMPTY;
    }

    public boolean deleteConfigMap(String name, String namespace) {
        log.info("delete => name:{},namespace:{}", name, namespace);
        boolean result = k8sClientHolder.getClient().configMaps().inNamespace(namespace).withName(name).delete();
        if (result) {
            this.delCache(name, namespace);
        }
        return result;
    }

    public boolean delete(String name, String namespace) {
        log.info("delete => name:{},namespace:{}", name, namespace);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        ConfigMap configMap = client.configMaps().inNamespace(namespace).withName(name).get();
        if (configMap != null) {
            client.configMaps().inNamespace(namespace).delete(configMap);
            this.delCache(name, namespace);
            return true;
        }
        return false;
    }

    public Boolean deleteWithLabelIn(String namespace, String key, String... values) {
        log.info("delete => key:{},values:{}", key, values);
        boolean result;
        if (StringUtils.isNotBlank(namespace)) {
            result = k8sClientHolder.getClient().configMaps().inNamespace(namespace).withLabelIn(key, values).delete();
        } else {
            result = k8sClientHolder.getClient().configMaps().inAnyNamespace().withLabelIn(key, values).delete();
        }
        if (result) {
            this.delCacheByLabel(namespace, key, values);
        }
        return result;
    }

    public void createOrReplace(String namespace, ConfigMap configMap) {
        log.info("update => namespace:{},configMap:{}", namespace, JSON.toJSONString(configMap));
        k8sClientHolder.getClient().configMaps().inNamespace(namespace).createOrReplace(configMap);
        this.patchCache(configMap);
    }

    public boolean createOrReplace(String name, String namespace, String yaml) {
        log.info("update => name:{},namespace:{},yaml:{}", name, namespace, yaml);
        try {
            DefaultKubernetesClient client = k8sClientHolder.getClient();
            ConfigMap configMap = new Yaml().loadAs(yaml, ConfigMap.class);
            configMap.getMetadata().setResourceVersion(null);
            if (log.isDebugEnabled()) {
                log.debug("configMap: " + JSON.toJSONString(configMap));
            }
//            client.configMaps().inNamespace(namespace).withName(name).delete();
            this.delCache(name, namespace);
            client.configMaps().inNamespace(namespace).withName(name).createOrReplace(configMap);
            this.patchCache(configMap);
            return true;
        } catch (Exception e) {
            log.error("patch k8sConfigmap by yaml found exception, k8sConfigmap:{}, namespace:{}, yaml:{}", name, namespace, yaml, e);
        }
        return false;
    }

    public void updateTcpOrUdpCm(String name, String namespace, ConfigMap configmap) {
        log.info("update => name:{},namespace:{},configmap:{}", name, namespace, JSON.toJSONString(configmap));
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        if (log.isDebugEnabled()) {
            log.debug("configMap: " + JSON.toJSONString(configmap));
        }
//        client.configMaps().inNamespace(namespace).withName(name).delete();
//        this.delCache(name, namespace);
        client.configMaps().inNamespace(namespace).withName(name).createOrReplace(configmap);
//        this.patchCache(configmap);
    }


    /**
     * 只用来更新cm的data
     *
     * @param name
     * @param namespace
     * @param data
     */
    public void createOrAppendData(String name, String namespace, Map<String, String> data) {
        log.info("get => name:{},namespace:{},data:{}", name, namespace, data);
        createOrAppendData(name, namespace, data, null, null);
    }

    /**
     * 创建或更新cm的data，label，annotation
     *
     * @param name
     * @param namespace
     * @param data
     * @param labels
     * @param annotations
     */
    public void createOrAppendData(String name, String namespace, Map<String, String> data,
                                   Map<String, String> labels, Map<String, String> annotations) {
        log.info("update => name:{},namespace:{},data:{},labels{},annotations{}", name, namespace, data, labels, annotations);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        ConfigMap configMap = client.configMaps().inNamespace(namespace).withName(name).get();
        configMap = configMap != null ? configMap : new ConfigMapBuilder().withNewMetadata().withNamespace(namespace).withName(name).endMetadata().build();
        configMap.setData(merge(configMap.getData(), data));
        configMap.getMetadata().setAnnotations(merge(getAnnotations(configMap.getMetadata().getAnnotations()), annotations));
        configMap.getMetadata().setLabels(merge(configMap.getMetadata().getLabels(), labels));
        if (log.isDebugEnabled()) {
            log.debug("configMap: " + JSON.toJSONString(configMap));
        }
        client.configMaps().inNamespace(namespace).createOrReplace(configMap);
        this.patchCache(configMap);
    }


    private <K, V> Map<K, V> merge(Map<K, V> map, Map<K, V> maps) {
        if (map == null) {
            map = Maps.newHashMap();
        }
        if (maps == null) {
            maps = Maps.newHashMap();
        }
        Map<K, V> result = new HashMap<>();
        result.putAll(map);
        result.putAll(maps);
        return result;
    }


    //------------------------------------------------------------------------以下为重构方法--------------------------------/
    @Override
    public ConfigMap toSimpleData(HasMetadata hasMetadata) {
        ConfigMap configMap = (ConfigMap) hasMetadata;
        configMap.getMetadata().setResourceVersion(null);
        configMap.getMetadata().setSelfLink(null);
        configMap.getMetadata().setUid(null);
        configMap.getMetadata().setManagedFields(null);
        configMap.getMetadata().setOwnerReferences(null);

        configMap.setData(null);
        return configMap;
    }

    @Override
    public List<ConfigMap> list() {
        log.info("get => list all");
        return k8sClientHolder.getClient().configMaps().inAnyNamespace().list().getItems();
    }

    @Override
    public List<ConfigMap> list(String namespace) {

        if (StringUtils.isBlank(namespace)) {
            return this.list();
        }
        log.info("get => namespace:{}", namespace);
        return k8sClientHolder.getClient().configMaps().inNamespace(namespace).list().getItems();
    }

    public ConfigMap toSimpleData(ConfigMap configMap) {
        return null;
    }

    public List<ConfigMap> list(Long limit) {
        log.info("get => limit:{}", limit);
        return k8sClientHolder.getClient().configMaps().inAnyNamespace().list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    public List<ConfigMap> list(String namespace, Long limit) {

        // 若值为空或小于0则默认100条
        if (limit == null || limit < 0) {
            return this.list(namespace);
        }

        // 空间为空时，查询当前集群下所有信息
        if (StringUtils.isBlank(namespace)) {
            return this.list(limit);
        }
        log.info("get => namespace:{} ,limit:{}", namespace, limit);
        return k8sClientHolder.getClient().configMaps().inNamespace(namespace).list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    public List<ConfigMap> get(String name) {
        log.info("get => name:{}", name);
        List<ConfigMap> result = new ArrayList<>();
        List<Namespace> items = k8sClientHolder.getClient().namespaces().list().getItems();
        if (CollectionUtils.isEmpty(items)) {
            return result;
        }

        for (Namespace item : items) {
            ConfigMap configMap = get(name, item.getMetadata().getName());
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
    public ConfigMap get(String name, String namespace) {
        return k8sClientHolder.getClient().configMaps().inNamespace(namespace).withName(name).get();
    }

    public List<ConfigMap> listByLabels(Map<String, String> labels) {
        log.info("get => labels:{}", labels);
        return k8sClientHolder.getClient().configMaps().inAnyNamespace().withLabels(labels).list().getItems();
    }

    public List<ConfigMap> listByLabels(String namespace, Map<String, String> labels) {
        log.info("get => namespace:{},labels:{}", namespace, labels);
        if (StringUtils.isEmpty(namespace)) {
            return this.listByLabels(labels);
        }
        return k8sClientHolder.getClient().configMaps().inNamespace(namespace).withLabels(labels).list().getItems();
    }

    public List<ConfigMap> getByLabels(Map<String, String> labels) {
        return k8sClientHolder.getClient().configMaps().inAnyNamespace().withLabels(labels).list().getItems();
    }

    public List<ConfigMap> listByLabels(List<String> labels) {
        log.info("get => labels:{}", labels);
        List<LabelSelectorRequirement> requirements = Lists.newArrayList();
        for (String label : labels) {
            requirements.add(new LabelSelectorRequirementBuilder()
                    .withKey(label).withOperator("Exists").build());
        }
        return k8sClientHolder.getClient().configMaps().inAnyNamespace().withLabelSelector(new LabelSelectorBuilder().withMatchExpressions(requirements).build()).list().getItems();
    }

    public List<ConfigMap> listByLabels(String namespace, List<String> labels) {
        if (StringUtils.isBlank(namespace)) {
            return this.listByLabels(labels);
        }
        List<LabelSelectorRequirement> requirements = Lists.newArrayList();
        for (String label : labels) {
            requirements.add(new LabelSelectorRequirementBuilder()
                    .withKey(label).withOperator("Exists").build());
        }
        return k8sClientHolder.getClient().configMaps().inNamespace(namespace).withLabelSelector(new LabelSelectorBuilder().withMatchExpressions(requirements).build()).list().getItems();
    }


    public List<ConfigMap> listByLabel(String namespace, String label, String... value) {
        if (StringUtils.isBlank(namespace)) {
            return this.listByLabelCustom(label, value);
        }
        log.info("get => namespace:{} ,label:{},value{}", namespace, value);
        return k8sClientHolder.getClient().configMaps().inAnyNamespace().withLabelIn(label, value).list().getItems();
    }


    public List<ConfigMap> listByLabelCustom(String label, String... value) {
        log.info("get => label:{},value{}", label, value);
        return k8sClientHolder.getClient().configMaps().inAnyNamespace().withLabelIn(label, value).list().getItems();
    }

    public void createOrReplace(ConfigMap configMap) {
        k8sClientHolder.getClient().configMaps().inNamespace(configMap.getMetadata().getNamespace()).createOrReplace(configMap);
    }

    public ConfigMap getConfigmap(String name, String namespace) {
        log.info("get configmap, param: name={}, namespace={}", name, namespace);
        return k8sClientHolder.getClient().configMaps().inNamespace(namespace).withName(name).get();
    }

}
