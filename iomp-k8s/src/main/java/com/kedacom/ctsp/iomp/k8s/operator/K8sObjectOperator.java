package com.kedacom.ctsp.iomp.k8s.operator;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import com.kedacom.ctsp.iomp.k8s.common.cache.K8sAbstractOperator;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Slf4j
public class K8sObjectOperator extends K8sAbstractOperator {

    public K8sObjectOperator(K8sClientHolder k8sClientHolder) {
        super(k8sClientHolder);
    }

    public List<HasMetadata> getAllObjWithLabelIn(String key, String... values) {
        log.info("get => key:{},values:{}", key,values);
        List<HasMetadata> all = Lists.newArrayList();
        all.addAll(k8sClientHolder.getClient().apps().deployments().withLabelIn(key, values).list().getItems());
        all.addAll(k8sClientHolder.getClient().apps().daemonSets().withLabelIn(key, values).list().getItems());
        all.addAll(k8sClientHolder.getClient().apps().statefulSets().withLabelIn(key, values).list().getItems());
        return all;
    }

    public void deleteWithLabelIn(String key, String... values) {
        log.info("delete => key:{},values:{}", key,values);
        k8sClientHolder.getClient().apps().deployments().withLabelIn(key, values).delete();
        k8sClientHolder.getClient().apps().statefulSets().withLabelIn(key, values).delete();
        List<DaemonSet> daemonSets = k8sClientHolder.getClient().apps().daemonSets().withLabelIn(key, values).list().getItems();
        for (DaemonSet ds : daemonSets) {
            // ds 得手动设置级联删除
            k8sClientHolder.getClient().apps().daemonSets().inNamespace(ds.getMetadata().getNamespace())
                    .withName(ds.getMetadata().getName()).cascading(true).delete();
            this.delCache(ds.getMetadata().getName(),ds.getMetadata().getNamespace());
        }
    }


}
