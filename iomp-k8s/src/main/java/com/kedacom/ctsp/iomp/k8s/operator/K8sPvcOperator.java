package com.kedacom.ctsp.iomp.k8s.operator;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import com.kedacom.ctsp.iomp.k8s.common.cache.K8sAbstractOperator;
import com.kedacom.ctsp.iomp.k8s.constant.K8sConstants;
import com.kedacom.ctsp.iomp.k8s.constant.K8sLabels;
import com.kedacom.ctsp.iomp.k8s.vo.K8sPvVo;
import com.kedacom.ctsp.iomp.k8s.vo.K8sPvcVo;
import com.kedacom.ctsp.lang.exception.CommonException;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.fabric8.kubernetes.api.model.storage.StorageClassBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class K8sPvcOperator extends K8sAbstractOperator {

    public K8sPvcOperator(K8sClientHolder k8sClientHolder) {
        super(k8sClientHolder);
    }

    public PersistentVolumeClaim createOrReplace(PersistentVolumeClaim pvc) {
        log.info("update => pvc{}", JSON.toJSONString(pvc));
        PersistentVolumeClaim result = k8sClientHolder.getClient().persistentVolumeClaims().inNamespace(pvc.getMetadata().getNamespace()).createOrReplace(pvc);
        if (result != null) {
            this.patchCache(pvc);
        }
        return result;
    }

    public PersistentVolumeClaim createOrReplace(String name, String namespace, PersistentVolumeClaim pvc) {
        log.info("update => name:{},namespace:{},pvc{}", name, namespace, JSON.toJSONString(pvc));
        PersistentVolumeClaim result = k8sClientHolder.getClient().persistentVolumeClaims().inNamespace(namespace).withName(name).createOrReplace(pvc);
        if (result != null) {
            this.patchCache(pvc);
        }
        return result;
    }


    public void createDefaultStorageClass(String name) {
        log.info("update => name:{}", name);
        Map<String, String> labels = Maps.newHashMap();
        labels.put("localpv", "true");
        StorageClassBuilder scb = new StorageClassBuilder();
        scb.withApiVersion(K8sConstants.API_VERSION_STORAGE)
                .withNewMetadata()
                .withName(name)
                .withLabels(labels)
                .endMetadata()
                .withProvisioner("kubernetes.io/no-provisioner")
                .withReclaimPolicy(K8sConstants.RETAIN);
        StorageClass storageClass = scb.build();
        if (log.isDebugEnabled()) {
            log.debug("StorageClass: " + JSON.toJSONString(storageClass));
        }
        // 无需缓存，不是PVC
        new K8sStorageClassOperator(this.k8sClientHolder).createOrReplace(storageClass);
//        k8sClientHolder.getClient().storage().storageClasses().createOrReplace(storageClass);

    }

    private void createStorageClass(String name) {
        log.info("update => name:{}", name);
        Map<String, String> labels = Maps.newHashMap();
        labels.put("localpv", "true");
        StorageClassBuilder scb = new StorageClassBuilder();
        scb.withApiVersion(K8sConstants.API_VERSION_STORAGE)
                .withNewMetadata()
                .withName(name)
                .withLabels(labels)
                .endMetadata()
                .withProvisioner("kubernetes.io/no-provisioner")
                .withVolumeBindingMode("WaitForFirstConsumer");
        StorageClass storageClass = scb.build();
        if (log.isDebugEnabled()) {
            log.debug("StorageClass: " + JSON.toJSONString(storageClass));
        }
        new K8sStorageClassOperator(this.k8sClientHolder).createOrReplace(storageClass);
//        k8sClientHolder.getClient().storage().storageClasses().createOrReplace(storageClass);
    }

    public PersistentVolume createPv(K8sPvVo vo) {
        log.info("update => vo:{}", JSON.toJSONString(vo));
        if (vo.getStorageSize() == null || StringUtils.isBlank(vo.getHostname())) {
            return null;
        }

        if (vo.getStorageClassName() == null) {
            return null;
        }

        final StorageClass storageClass = k8sClientHolder.getClient().storage().storageClasses().withName(vo.getStorageClassName()).get();
        if (storageClass == null) {
            if (vo.isWaitForFirstConsumer()) {
                createStorageClass(vo.getStorageClassName());
            } else {
                createDefaultStorageClass(vo.getStorageClassName());
            }
        }
        PersistentVolumeBuilder pb = new PersistentVolumeBuilder();

        Map<String, Quantity> limits = new HashMap<>();
        limits.put(K8sConstants.STORAGE, new Quantity(vo.getStorageSize() + "Mi"));

        NodeSelectorTerm nodeSelectorTerm = new NodeSelectorTerm();
        nodeSelectorTerm.getMatchExpressions().add(
                new NodeSelectorRequirement("kubernetes.io/hostname",
                        "In", Arrays.asList(vo.getHostname())));

        pb.withApiVersion(K8sConstants.API_VERSION_V1)
                .withNewMetadata()
                .withName(vo.getName())
                .withLabels(vo.getLabels())
                .endMetadata()
                .withNewSpec()
                .withStorageClassName(vo.getStorageClassName())
                .withCapacity(limits)
                .withAccessModes(K8sConstants.ACCESS_MODES_RWO)
                .withVolumeMode("Filesystem")
                .withPersistentVolumeReclaimPolicy("Retain")
                .withNewLocal()
                .withPath(vo.getPath())
                .endLocal()
                .withNewNodeAffinity()
                .withNewRequired()
                .withNodeSelectorTerms(nodeSelectorTerm)
                .endRequired()
                .endNodeAffinity()
                .endSpec();
        if (log.isDebugEnabled()) {
            log.debug("persistentVolume: " + JSON.toJSONString(pb.build()));
        }
        PersistentVolume pv = new K8sPvOperator(this.k8sClientHolder).createOrReplace(pb.build());
        this.patchCache(pv);
        return pv;

    }


    public void createPvc(K8sPvcVo vo, String accessModes) {
        log.info("update => K8sPvcVo:{}", JSON.toJSON(vo));
        String storageClassName = vo.getStorageClassName();
        if (k8sClientHolder.getClient().storage().storageClasses().withName(storageClassName) == null
                || k8sClientHolder.getClient().storage().storageClasses().withName(storageClassName).get() == null) {
            throw new CommonException("未找到对应的storageClass");
        }

        Map<String, Quantity> limits = new HashMap<>();
        limits.put(K8sConstants.STORAGE, new Quantity(vo.getStorageSize()));

        Map<String, String> labels = Maps.newHashMap();
        labels.put("client_id", vo.getClient_id());

        PersistentVolumeClaimBuilder pb = new PersistentVolumeClaimBuilder();
        if (StringUtils.isBlank(vo.getPvName())) {
            pb.withApiVersion(K8sConstants.API_VERSION_V1)
                    .withNewMetadata()
                    .withName(vo.getName())
                    .withNamespace(vo.getNamespace())
                    .withLabels(labels)
                    .endMetadata()
                    .withNewSpec()
                    .withAccessModes(accessModes)
                    .withNewResources()
                    .withRequests(limits).endResources()
                    .withStorageClassName(storageClassName).endSpec();
        } else {
            labels.put(K8sLabels.LABEL_STORAGE_SOURCE, "localpv");
            pb.withApiVersion(K8sConstants.API_VERSION_V1)
                    .withNewMetadata()
                    .withName(vo.getName())
                    .withNamespace(vo.getNamespace())
                    .withLabels(labels)
                    .endMetadata()
                    .withNewSpec()
                    .withAccessModes(accessModes)
                    .withVolumeMode(K8sConstants.VOLUME_MODE_FILE)
                    .withVolumeName(vo.getPvName())
                    .withNewResources()
                    .withRequests(limits).endResources()
                    .withStorageClassName(storageClassName).endSpec();

        }
        k8sClientHolder.getClient().persistentVolumeClaims().inNamespace(vo.getNamespace()).createOrReplace(pb.build());
    }


    public boolean delete(PersistentVolumeClaim pvc) {
        log.info("delete => pvc:{}, pvc");
        boolean result = k8sClientHolder.getClient().persistentVolumeClaims().inNamespace(pvc.getMetadata().getNamespace()).delete(pvc);
        if (result) {
            this.delCache(pvc.getMetadata().getName(), pvc.getMetadata().getNamespace());
        }
        return result;
    }

    public boolean delete(String name, String namespace) {
        log.info("delete => name:{},namespace:{}", name, namespace);
        boolean result = k8sClientHolder.getClient().persistentVolumeClaims().inNamespace(namespace).withName(name).delete();
        if (result) {
            this.delCache(name, namespace);
        }
        return result;
    }

    //------------------------------------------------------------------------以下为重构方法--------------------------------/
    @Override
    public PersistentVolumeClaim toSimpleData(HasMetadata hasMetadata) {
        PersistentVolumeClaim pvc = (PersistentVolumeClaim) hasMetadata;
        pvc.getMetadata().setResourceVersion(null);
        pvc.getMetadata().setSelfLink(null);
        pvc.getMetadata().setUid(null);
        pvc.getMetadata().setManagedFields(null);
        pvc.getMetadata().setOwnerReferences(null);

        return pvc;
    }

    @Override
    public List<PersistentVolumeClaim> list() {
        log.info("get => list all");
        return k8sClientHolder.getClient().persistentVolumeClaims().inAnyNamespace().list().getItems();
    }

    @Override
    public List<PersistentVolumeClaim> list(String namespace) {

        if (StringUtils.isBlank(namespace)) {
            return this.list();
        }
        log.info("get => namespace:{}", namespace);
        return k8sClientHolder.getClient().persistentVolumeClaims().inNamespace(namespace).list().getItems();
    }

    public List<PersistentVolumeClaim> list(Long limit) {
        log.info("get => limit:{}", limit);
        return k8sClientHolder.getClient().persistentVolumeClaims().inAnyNamespace().list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    public List<PersistentVolumeClaim> list(String namespace, Long limit) {

        // 若值为空或小于0则默认100条
        if (limit == null || limit < 0) {
            return this.list(namespace);
        }

        // 空间为空时，查询当前集群下所有信息
        if (StringUtils.isBlank(namespace)) {
            return this.list(limit);
        }
        log.info("get => namespace:{} ,limit:{}", namespace, limit);
        return k8sClientHolder.getClient().persistentVolumeClaims().inNamespace(namespace).list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    /**
     * 根据名称获取对象
     *
     * @param namespace 必填，否则无法查询name
     * @param name
     * @return
     */
    public PersistentVolumeClaim get(String name, String namespace) {
        log.info("get => namespace:{} ,name:{}", namespace, name);
        return k8sClientHolder.getClient().persistentVolumeClaims().inNamespace(namespace).withName(name).get();
    }

}
