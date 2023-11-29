package com.kedacom.ctsp.iomp.k8s.operator;


import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import com.kedacom.ctsp.iomp.k8s.common.cache.K8sAbstractOperator;
import com.kedacom.ctsp.iomp.k8s.vo.K8sSecretDto;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

/**
 * @author Administrator
 */
@Slf4j
public class K8sSecretOperator extends K8sAbstractOperator {

    /**
     * tls secret type
     */
    public static final String SECRET_TLS_TYPE = "kubernetes.io/tls";
    /**
     * tls secret crt
     */
    public static final String SECRET_TLS_CRT = "tls.crt";
    /**
     * tls secret key
     */
    public static final String SECRET_TLS_KEY = "tls.key";
    /**
     * tls secret p12
     */
    public static final String SECRET_TLS_P12 = "tls.p12";
    /**
     * tls secret pem
     */
    public static final String SECRET_TLS_PEM = "tls.pem";
    /**
     * ca.crt
     */
    public static final String SECRET_CA_CRT = "ca.crt";

    public static final String SECRET_CA_KEY = "ca.key";

    /**
     * 镜像仓库证书名称
     */
    public static final String REGISTRY_CACERT = "cacert.crt";

    public static final String REGISTRY_CLIENGCERT = "client.cert";

    public static final String REGISTRY_CLIENGKEY = "client.key";

    public static final String SECRET_KUBECTL_CONFIG = "config";

    public K8sSecretOperator(K8sClientHolder k8sClientHolder) {
        super(k8sClientHolder);
    }

    public List<Secret> getWithLabelIn(String key, String namespace, String... values) {
        if (StringUtils.isEmpty(namespace)) {
            SecretList secretList = k8sClientHolder.getClient().secrets().inAnyNamespace().withLabelIn(key, values).list(new ListOptionsBuilder().withLimit(10L).build());
            return secretList != null ? secretList.getItems() : Lists.newArrayList();
        } else {
            return getSecretByLabelsCustom(key, namespace, values);
        }
    }

    public List<Secret> getSecretByLabelsCustom(String key, String namespace, String... values) {
        log.info("get => key:{},namespace:{},values:{}", key, namespace, values);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        SecretList secretList = client.secrets().inNamespace(namespace).withLabelIn(key, values).list();
        if (secretList == null) {
            return Lists.newArrayList();
        }
        return secretList.getItems();
    }


    public void createClientSecret(String deployName, String clientSecret, Map<String, String> labels, String namespace) {
        log.info("update => deployName:{},clientSecret:{},labels:{},namespace:{}", deployName, clientSecret, JSON.toJSONString(labels), namespace);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        Secret secret = this.get(deployName, namespace);
        if (secret == null) {
            secret = new Secret();
            //设置名称
            ObjectMeta metadata = new ObjectMeta();
            metadata.setName(deployName);
            metadata.setNamespace(namespace);
            metadata.setLabels(labels);
            secret.setMetadata(metadata);
            secret.setType("Opaque");
        }

        Map<String, String> dataMap = secret.getData() == null ? new HashMap<>() : secret.getData();
        dataMap.put("client_id", Base64.getEncoder().encodeToString(deployName.getBytes()));
        dataMap.put("client_secret", Base64.getEncoder().encodeToString(clientSecret.getBytes()));
        secret.setData(dataMap);
        if (log.isDebugEnabled()) {
            log.debug("secret: " + JSON.toJSONString(secret));
        }
        client.secrets().inNamespace(namespace).createOrReplace(secret);
        this.patchCache(secret);
    }

    public void createSecret(K8sSecretDto secretDto) {
        log.info("update => secretDto:{}", JSON.toJSONString(secretDto));
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        Secret secret = new Secret();

        Map<String, String> dataMap = secretDto.getData();
        if (dataMap == null) {
            dataMap = new HashMap<>();
        }

        // Secret必须包含tls.key，tls.crt且必须有值
        if (StringUtils.isNotBlank(secretDto.getCrtData())) {
            dataMap.put(SECRET_TLS_CRT, secretDto.getCrtData());
        }
        if (StringUtils.isNotBlank(secretDto.getKeyData())) {
            dataMap.put(SECRET_TLS_KEY, secretDto.getKeyData());
        }
        if (StringUtils.isNotEmpty(secretDto.getCertFormat())) {
            String[] types = secretDto.getCertFormat().split(",");
            for (String type : types) {
                switch (type) {
                    case "CRT":
                        // 默认已经全部挂载，无需继续挂载
                        break;
                    case "P12":
                        dataMap.put(SECRET_TLS_P12, secretDto.getP12Data());
                        break;
                    case "PEM":
                        dataMap.put(SECRET_TLS_PEM, secretDto.getPemData());
                        break;
                }
            }
        }

        secret.setData(dataMap);

        //设置名称
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName(secretDto.getName());
        metadata.setNamespace(secretDto.getNamespace());
        metadata.setLabels(secretDto.getLabels());
        metadata.setAnnotations(secretDto.getAnnotations());
        secret.setMetadata(metadata);
        secret.setType(SECRET_TLS_TYPE);
        client.secrets().inNamespace(secretDto.getNamespace()).withName(secretDto.getName()).delete();
        if (log.isDebugEnabled()) {
            log.debug("secret: " + JSON.toJSONString(secret));
        }
        client.secrets().inNamespace(secretDto.getNamespace()).createOrReplace(secret);
        this.patchCache(secret);
    }

    // TODO: 2021/12/26 特殊方法
    public SecretList getAllSecret(String namespace) {
        log.info("get => namespace:{}", namespace);
        return k8sClientHolder.getClient().secrets().inNamespace(namespace).list();
    }

    /**
     * 删除secret
     *
     * @param name      secret名称
     * @param namespace 空间名称
     * @return
     */
    public void deleteSecret(String name, String namespace) {
        log.info("delete => name:{},nameSpace:{}", name, namespace);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        client.secrets().inNamespace(namespace).withName(name).delete();
        this.delete(name, namespace);
    }

    public boolean createOrReplace(String name, String namespace, String yaml) {
        log.info("update => name:{},nameSpace:{},yaml", name, namespace, yaml);
        try {
            DefaultKubernetesClient client = k8sClientHolder.getClient();
            Secret secret = client.secrets().inNamespace(namespace).withName(name).get();
            if (secret != null) {
                Secret newSecret = new Yaml().loadAs(yaml, Secret.class);
                newSecret.getMetadata().setResourceVersion(null);
                if (log.isDebugEnabled()) {
                    log.info("secret: " + JSON.toJSONString(secret));
                }
                client.secrets().inNamespace(namespace).createOrReplace(newSecret);
                this.patchCache(newSecret);
                return true;
            }
        } catch (Exception e) {
            log.error("patch k8sSecret by yaml found exception, k8sSecret:{}, namespace:{}, yaml:{}", name, namespace, yaml, e);
        }
        return false;
    }

    public boolean delete(String name, String namespace) {
        log.info("delete => name:{},nameSpace:{}", name, namespace);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        boolean result = client.secrets().inNamespace(namespace).withName(name).delete();
        if (result) {
            this.delCache(name, namespace);
        }
        return result;
    }

    public Boolean deleteWithLabelIn(String namespace, String key, String... values) {
        log.info("delete => namespace:{},key:{},values:{}", key, values);
        boolean result;
        if (StringUtils.isNotBlank(namespace)) {
            result = k8sClientHolder.getClient().secrets().inNamespace(namespace).withLabelIn(key, values).delete();
        } else {
            result = k8sClientHolder.getClient().secrets().withLabelIn(key, values).delete();
        }
        if (result) {
            this.delCacheByLabel(namespace, key, values);
        }
        return result;
    }

    public Secret createOrReplace(String namespace, Secret secret) {
        log.info("update => namespace:{},secret:{}", namespace, JSON.toJSONString(secret));
        Secret result = k8sClientHolder.getClient().secrets().inNamespace(namespace).createOrReplace(secret);
        if (result != null) {
            this.patchCache(secret);
        }
        return result;
    }

    public void appendAnnotations(String secretName, String namespace, Map<String, String> annos) {
        log.info("update => secretName:{},namespace:{},annos{}", secretName, namespace, JSON.toJSONString(annos));
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        Secret secret = client.secrets().inNamespace(namespace).withName(secretName).get();
        if (secret == null) {
            return;
        }
        Map<String, String> annotations = secret.getMetadata().getAnnotations();
        if (annotations == null) {
            annotations = Maps.newHashMap();
        }
        Iterator<Map.Entry<String, String>> iterator = annos.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> next = iterator.next();
            annotations.put(next.getKey(), next.getValue());
        }
        secret.getMetadata().setAnnotations(annotations);
        if (log.isDebugEnabled()) {
            log.debug("secret:{}", JSON.toJSONString(secret));
        }
        client.secrets().inNamespace(namespace).createOrReplace(secret);
        if (log.isDebugEnabled()) {
            log.debug("secret: " + JSON.toJSONString(secret));
        }
    }


    /**
     * 1.先根据全等来匹配
     * 2.再根据带*的来匹配
     *
     * @param domain
     * @return
     */
    public Secret getWithDomain(String domain) {
        log.info("get => domain:{}", domain);
        List<Secret> secretList = listByLabelCustom("cert/type", "domain");
        for (Secret secret : secretList) {
            Map<String, String> annotations = secret.getMetadata().getAnnotations();
            if (annotations != null && annotations.get("cert/domain") != null) {
                String[] split = StringUtils.split(annotations.get("cert/domain"), ",");
                if (Arrays.asList(split).contains(domain)) {
                    return secret;
                }
            }
        }
        //走到这来，说明上面没匹配上
        for (Secret secret : secretList) {
            Map<String, String> annotations = secret.getMetadata().getAnnotations();
            if (annotations != null && annotations.get("cert/domain") != null) {
                String[] split = StringUtils.split(annotations.get("cert/domain"), ",");
                for (String certDomain : split) {
                    if (StringUtils.startsWith(certDomain, "*")
                            && StringUtils.endsWith(domain, StringUtils.substringAfter(certDomain, "*"))) {
                        return secret;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 拷贝secret
     * 1.目标空间已存在，不进行拷贝
     * 2.未找到 srcSecret 那么就不进行拷贝
     *
     * @param srcSecretName
     * @param srcNamespace
     * @param destNamespace
     */
    public void copy(String srcSecretName, String srcNamespace, String destNamespace) {
        log.info("get => srcSecretName:{},srcNamespace:{},destNamespace{}", srcSecretName, srcNamespace, destNamespace);
        try {
            Secret destSecret = get(srcSecretName, destNamespace);
            if (destSecret != null) {
                return;
            }
            Secret srcSecret = get(srcSecretName, srcNamespace);
            if (srcSecret == null) {
                return;
            }
            srcSecret.getMetadata().setNamespace(destNamespace);
            Map<String, String> labels = srcSecret.getMetadata().getLabels();
            labels = labels == null ? Maps.newHashMap() : labels;
            labels.put("app.kubernetes.io/managed-by", "iomp-api");
            srcSecret.getMetadata().setLabels(labels);
            srcSecret.getMetadata().setResourceVersion(null);
            srcSecret.getMetadata().setUid(null);
            if (log.isDebugEnabled()) {
                log.debug("srcSecret: " + JSON.toJSONString(srcSecret));
            }
            // wuqiusheng
            k8sClientHolder.getClient().secrets().inNamespace(destNamespace).createOrReplace(srcSecret);
//            k8sClientHolder.getClient().secrets().createOrReplace(srcSecret);
            this.patchCache(srcSecret);
        } catch (Exception e) {
            log.info("copy secret {}.{} to {} error ,{}", srcSecretName, srcNamespace, destNamespace, e);
        }
    }

    //------------------------------------------------------------------------以下为重构方法--------------------------------/
    @Override
    public Secret toSimpleData(HasMetadata hasMetadata) {
        Secret secret = (Secret) hasMetadata;
        secret.getMetadata().setResourceVersion(null);
        secret.getMetadata().setSelfLink(null);
        secret.getMetadata().setManagedFields(null);
        secret.getMetadata().setOwnerReferences(null);

        secret.setData(null);
        return secret;
    }

    @Override
    public List<Secret> list() {
        log.info("get => list all");
        return k8sClientHolder.getClient().secrets().inAnyNamespace().withLabelNotIn("owner", "helm").list().getItems();
    }

    @Override
    public List<Secret> list(String namespace) {

        if (StringUtils.isBlank(namespace)) {
            return this.list();
        }
        log.info("get => namespace:{}", namespace);
        return k8sClientHolder.getClient().secrets().inNamespace(namespace).withLabelNotIn("owner", "helm").list().getItems();
    }

    public List<Secret> list(Long limit) {
        log.info("get => limit:{}", limit);
        return k8sClientHolder.getClient().secrets().inAnyNamespace().withLabelNotIn("owner", "helm").list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    public List<Secret> list(String namespace, Long limit) {

        // 若值为空或小于0则默认100条
        if (limit == null || limit < 0) {
            return this.list(namespace);
        }

        // 空间为空时，查询当前集群下所有信息
        if (StringUtils.isBlank(namespace)) {
            return this.list(limit);
        }
        log.info("get => namespace:{} ,limit:{}", namespace, limit);
        return k8sClientHolder.getClient().secrets().inNamespace(namespace).withLabelNotIn("owner", "helm").list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }


    /**
     * 根据名称获取对象
     *
     * @param namespace 必填，否则无法查询name
     * @param name
     * @return
     */
    public Secret get(String name, String namespace) {
        log.info("get => name:{} ,namespace:{}", name, namespace);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        int oldTimeout = client.getConfiguration().getRequestTimeout();
        try {
            client.getConfiguration().setRequestTimeout(20000);
            return client.secrets().inNamespace(namespace).withName(name).get();
        } finally {
            client.getConfiguration().setRequestTimeout(oldTimeout);
        }
    }

    /**
     * 根据名称获取对象
     *
     * @param namespaces 必填，否则无法查询name
     * @param name
     * @return
     */
    public Secret get(String name, String... namespaces) {
//        log.info("get => name:{} ,namespace:{}", name, namespace);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        int oldTimeout = client.getConfiguration().getRequestTimeout();
        try {
            client.getConfiguration().setRequestTimeout(20000);
            for (String namespace : namespaces) {
                Secret secret = client.secrets().inNamespace(namespace).withName(name).get();
                if (secret != null) {
                    return secret;
                }
            }
        } finally {
            client.getConfiguration().setRequestTimeout(oldTimeout);
        }
        return null;
    }


    public List<Secret> listByLabelCustom(String label, String... value) {
        log.info("get => label:{},value{}", label, value);
        return k8sClientHolder.getClient().secrets().inAnyNamespace().withLabelIn(label, value).list().getItems();
    }

}
