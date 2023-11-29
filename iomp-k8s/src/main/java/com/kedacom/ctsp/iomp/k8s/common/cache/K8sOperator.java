package com.kedacom.ctsp.iomp.k8s.common.cache;

import io.fabric8.kubernetes.api.model.HasMetadata;

import java.util.List;

/**
 * @author keda
 */
public interface K8sOperator {

    List<? extends HasMetadata> list();

    List<? extends HasMetadata> list(String namespace);

    void refreshCache();

    <T extends HasMetadata> void patchCache(T metadata);

    void patchCache(List metadata);

    void delCache(String name);

    void delCache(String name, String namespace);

    void delCacheByLabel(String namespace,String key,String...labels);

    <T extends HasMetadata> T toSimpleData(T metadata);
}
