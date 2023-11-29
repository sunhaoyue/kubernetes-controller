package com.kedacom.ctsp.iomp.k8s.operator;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import com.kedacom.ctsp.iomp.k8s.common.cache.K8sAbstractOperator;
import com.kedacom.ctsp.iomp.k8s.common.util.IPUtils;
import com.kedacom.ctsp.iomp.k8s.common.util.JoinerUtil;
import com.kedacom.ctsp.iomp.k8s.vo.K8sLocalpvVo;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * PV operator
 *
 * @author Xiang Zhou
 * @create 2020-08-19 16:18
 **/
@Slf4j
public class K8sPvOperator extends K8sAbstractOperator {

    public K8sPvOperator(K8sClientHolder k8sClientHolder) {
        super(k8sClientHolder);
    }

    public PersistentVolume createOrReplace(PersistentVolume pv) {
        log.info("update =>pv:{}", JSON.toJSONString(pv));
        PersistentVolume result = this.k8sClientHolder.getClient().persistentVolumes().createOrReplace(pv);
        if (result != null) {
            this.patchCache(pv);
        }
        return result;
    }

    public PersistentVolume create(String name, PersistentVolume pv) {
        log.info("update => name:{},pv:{}", name, JSON.toJSONString(pv));
        return this.k8sClientHolder.getClient().persistentVolumes().withName(name).create(pv);
    }

    public PersistentVolume createOrReplace(String name, PersistentVolume pv) {
        log.info("update => name:{},pv:{}", name, JSON.toJSONString(pv));
        PersistentVolume result = this.k8sClientHolder.getClient().persistentVolumes().withName(name).createOrReplace(pv);
        if (result != null) {
            this.patchCache(pv);
        }
        return result;
    }

    public boolean delete(PersistentVolume pv) {
        log.info("delete => pv:{}", pv);
        boolean reslut = k8sClientHolder.getClient().persistentVolumes().delete(pv);
        if (reslut) {
            this.delCache(pv.getMetadata().getName(), pv.getMetadata().getNamespace());
        }
        return reslut;
    }

    public boolean delete(String name) {
        log.info("delete => name:{}", name);
        boolean reslut = k8sClientHolder.getClient().persistentVolumes().withName(name).delete();
        if (reslut) {
            this.delCache(name);
        }
        return reslut;
    }

    public List<K8sLocalpvVo> queryByK8sName(String clusterId, String k8sName, String namespace) {
        log.info("get => clusterId:{},k8sName:{},namespace{}", clusterId, k8sName, namespace);
        StatefulSet sts = k8sClientHolder.getClient().apps().statefulSets().inNamespace(namespace).withName(k8sName).get();
        if (sts == null) {
            return Lists.newArrayList();
        }
        return getLocalpvWithContainer(clusterId, sts);
    }


    private List<K8sLocalpvVo> getLocalpvWithContainer(String clusterId, StatefulSet sts) {
        log.info("get => clusterId:{},sts:{}", clusterId, JSON.toJSONString(sts));
        List<PersistentVolumeClaim> pvcList = sts.getSpec().getVolumeClaimTemplates();
        if (CollectionUtils.isEmpty(pvcList)) {
            return Lists.newArrayList();
        }
        List<Container> containers = sts.getSpec().getTemplate().getSpec().getContainers();
        List<K8sLocalpvVo> storages = Lists.newArrayList();
        try {
            List<VolumeMount> volumeMounts = Lists.newArrayList();
            for (Container container : containers) {
                volumeMounts.addAll(container.getVolumeMounts());
            }
            DefaultKubernetesClient client = k8sClientHolder.getClient();
            List<PersistentVolume> items = client.persistentVolumes().list().getItems();
            for (PersistentVolumeClaim pvcTemplate : pvcList) {
                String storageClassName = pvcTemplate.getSpec().getStorageClassName();
                //本地存储路径
                String localPath = null;
                //容器挂载路径
                String mountPath = null;
                List<String> hostIps = Lists.newArrayList();
                for (PersistentVolume pv : items) {
                    if (StringUtils.equals(pv.getSpec().getStorageClassName(), storageClassName)) {
                        localPath = pv.getSpec().getLocal().getPath();
                        String pvHostName = pv.getSpec().getNodeAffinity().getRequired().getNodeSelectorTerms().get(0).getMatchExpressions().get(0).getValues().get(0);
                        try {
                            hostIps.add(IPUtils.getInternalIp(client.nodes().withName(pvHostName).get()));
                        } catch (Exception e) {
                            log.error(String.format("解析node [%s] 真实ip失败", pvHostName));
                        }
                    }
                }
                for (VolumeMount vm : volumeMounts) {
                    if (!StringUtils.equals(vm.getName(), storageClassName)) {
                        continue;
                    }
                    mountPath = vm.getMountPath();
                    break;
                }
                K8sLocalpvVo storageVo = new K8sLocalpvVo();
                storageVo.setClusterId(clusterId);
                storageVo.setK8sName(sts.getMetadata().getName());
                storageVo.setNamespace(sts.getMetadata().getNamespace());
                storageVo.setMountPath(mountPath);
                storageVo.setHostPath(localPath);
                storageVo.setHostIps(JoinerUtil.joinList(hostIps));
                storages.add(storageVo);
            }
        } catch (Exception e) {
            log.error("get localpv error", e);
        }
        return storages;
    }

    //------------------------------------------------------------------------以下为重构方法--------------------------------/
    @Override
    public PersistentVolume toSimpleData(HasMetadata hasMetadata) {
        PersistentVolume pv = (PersistentVolume) hasMetadata;
        pv.getMetadata().setResourceVersion(null);
        pv.getMetadata().setSelfLink(null);
        pv.getMetadata().setUid(null);
        pv.getMetadata().setManagedFields(null);
        pv.getMetadata().setOwnerReferences(null);
        //pv.getSpec().setNodeAffinity(null);
        return pv;
    }

    @Override
    public List<PersistentVolume> list() {
        log.info("get => list all");
        return k8sClientHolder.getClient().persistentVolumes().list().getItems();

    }

    @Override
    public List<PersistentVolume> list(String namespace) {
        return this.list();
    }

    public List<PersistentVolume> list(Long limit) {

        // 若值为空或小于0则默认100条
        if (limit == null || limit < 0) {
            return this.list();
        }
        log.info("get => limit:{}", limit);
        return k8sClientHolder.getClient().persistentVolumes().list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    /**
     * 根据名称获取对象
     *
     * @param name
     * @return
     */
    public PersistentVolume get(String name) {
        log.info("get => name:{}", name);
        return k8sClientHolder.getClient().persistentVolumes().withName(name).get();
    }

    public List<PersistentVolume> listByLabelCustom(String label, String... value) {
        log.info("get => label:{},value{}", label, value);
        return k8sClientHolder.getClient().persistentVolumes().withLabelIn(label, value).list().getItems();
    }
}