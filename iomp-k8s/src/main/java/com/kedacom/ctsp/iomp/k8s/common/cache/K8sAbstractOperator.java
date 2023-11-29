package com.kedacom.ctsp.iomp.k8s.common.cache;

import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import io.fabric8.kubernetes.api.model.HasMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * @author keda
 */
public abstract class K8sAbstractOperator implements K8sOperator {

    protected final K8sClientHolder k8sClientHolder;

    public K8sAbstractOperator(K8sClientHolder k8sClientHolder) {
        this.k8sClientHolder = k8sClientHolder;
        this.k8sClientHolder.put(this);
    }

    public K8sOperator getK8sCacheOperator() {
        return k8sClientHolder.getCacheOperator(this.getClass());
    }


    @Override
    public List<? extends HasMetadata> list() {
        return new ArrayList<>();
    }

    @Override
    public List<? extends HasMetadata> list(String namespace) {
        return new ArrayList<>();
    }

    @Override
    public void refreshCache() {

    }

    @Override
    public final <T extends HasMetadata> void patchCache(T metadata) {
        List tmp = new ArrayList();
        tmp.add(metadata);
        this.patchCache(tmp);
    }

    @Override
    public final void patchCache(List metadata) {
        if (k8sClientHolder.useCache()) {
            K8sOperator k8sOperator = getK8sCacheOperator();
            if (k8sOperator != null) {
                k8sOperator.patchCache(metadata);
            }
        }
    }


    @Override
    public final void delCache(String name) {
        if (k8sClientHolder.useCache()) {
            K8sOperator k8sOperator = getK8sCacheOperator();
            if (k8sOperator != null) {
                k8sOperator.delCache(name, null);
            }
        }
    }

    @Override
    public final void delCache(String name, String namespace) {
        if (k8sClientHolder.useCache()) {
            K8sOperator k8sOperator = getK8sCacheOperator();
            if (k8sOperator != null) {
                k8sOperator.delCache(name, namespace);
            }
        }
    }


    @Override
    public final void delCacheByLabel(String namespace, String key, String... labels) {
        if (k8sClientHolder.useCache()) {
            K8sOperator k8sOperator = getK8sCacheOperator();
            if (k8sOperator != null) {
                k8sOperator.delCacheByLabel(namespace, key, labels);
            }
        }
    }

    @Override
    public <T extends HasMetadata> T toSimpleData(T metadata) {
        return metadata;
    }

}
