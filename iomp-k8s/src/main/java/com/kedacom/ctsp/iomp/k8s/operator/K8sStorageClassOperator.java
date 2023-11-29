package com.kedacom.ctsp.iomp.k8s.operator;

import com.alibaba.fastjson.JSON;
import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import com.kedacom.ctsp.iomp.k8s.common.cache.K8sAbstractOperator;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Slf4j
public class K8sStorageClassOperator extends K8sAbstractOperator {

    public K8sStorageClassOperator(K8sClientHolder k8sClientHolder) {
        super(k8sClientHolder);
    }


    public List<StorageClass> getStorageClassList() {
        return k8sClientHolder.getClient().storage().storageClasses().list().getItems();
    }

    public boolean delete(String name) {
        log.info("delete => name:{}", name);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        boolean result = client.storage().storageClasses().withName(name).delete();
        if(result){
            this.delCache(name);
        }
        return result;
    }



    public void createOrReplace(StorageClass storageClass) {
        log.info("update => storageClass:{}", JSON.toJSONString(storageClass));
        String namespace = storageClass.getMetadata().getNamespace();
        if (StringUtils.isNotBlank(namespace)) {
            k8sClientHolder.getClient().inNamespace(namespace).storage().storageClasses().createOrReplace(storageClass);
        } else {
            k8sClientHolder.getClient().storage().storageClasses().createOrReplace(storageClass);
        }
        this.patchCache(storageClass);
    }

    //------------------------------------------------------------------------以下为重构方法--------------------------------/

    public List<StorageClass> list() {
        log.info("get => list all");
        return k8sClientHolder.getClient().storage().storageClasses().list().getItems();

    }

    @Override
    public List<StorageClass> list(String namespace) {
        return this.list();
    }

    public List<StorageClass> list(Long limit) {

        // 若值为空或小于0则默认100条
        if (limit == null || limit < 0) {
            return this.list();
        }
        log.info("get => limit:{}", limit);
        return k8sClientHolder.getClient().storage().storageClasses().list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    /**
     * 根据名称获取对象
     * @param name
     * @return
     */
    public StorageClass get(String name) {
        log.info("get => name:{}", name);
        return k8sClientHolder.getClient().storage().storageClasses().withName(name).get();
    }

    public  List<StorageClass> listByLabels(Map<String, String> labels){
        log.info("get => labels:{}", labels);
        return k8sClientHolder.getClient().storage().storageClasses().withLabels(labels).list().getItems();
    }

    public boolean deleteByName(String storageClassName){
        log.info("delete storageClass => storageClassName:{}", storageClassName);
        return k8sClientHolder.getClient().storage().storageClasses().withName(storageClassName).delete();
    }
}
