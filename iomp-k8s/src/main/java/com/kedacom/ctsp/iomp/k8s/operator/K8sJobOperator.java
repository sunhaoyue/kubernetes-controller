package com.kedacom.ctsp.iomp.k8s.operator;

import com.alibaba.fastjson.JSON;
import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import com.kedacom.ctsp.iomp.k8s.common.cache.K8sAbstractOperator;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1beta1.CronJob;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class K8sJobOperator extends K8sAbstractOperator {

    public K8sJobOperator(K8sClientHolder k8sClientHolder) {
       super(k8sClientHolder);
    }

    public boolean delete(String name, String namespace) {
        log.info("delete => name:{},nameSpace:{}", name, namespace);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        boolean result = client.batch().jobs().inNamespace(namespace).withName(name).delete();
        if(result){
            this.delCache(name,namespace);
        }
        return result;
    }

    public boolean delete(List<Job> jobs,String namespace) {
        log.info("delete => jobs:{}", JSON.toJSONString(jobs));
        boolean result = k8sClientHolder.getClient().batch().jobs().inNamespace(namespace).delete(jobs);
        if(result){
            if(!CollectionUtils.isEmpty(jobs)){
                jobs.forEach(t->{
                    this.delCache(t.getMetadata().getName(),t.getMetadata().getName());
                });
            }
        }
        return result;
//        return k8sClientHolder.getClient().batch().jobs().inNamespace(namespace).delete(jobs);
    }

    public Boolean deleteWithLabelIn(String namespace,String key, String... values) {
        log.info("delete => namespace:{},key:{},values:{}", key, values);
        boolean result;
        if(StringUtils.isNotBlank(namespace)){
            result = k8sClientHolder.getClient().batch().jobs().inNamespace(namespace).withLabelIn(key, values).delete();
        }else{
            result = k8sClientHolder.getClient().batch().jobs().withLabelIn(key, values).delete();
        }
        if(result){
            this.delCacheByLabel(namespace,key,values);
        }
        return result;
    }


    public void createOrReplace(String namespace, Job job) {
        log.info("update => namespace:{},job:{}", namespace, JSON.toJSONString(job));
        k8sClientHolder.getClient().batch().jobs().inNamespace(namespace).createOrReplace(job);
        this.patchCache(job);
    }



    //------------------------------------------------------------------------以下为重构方法--------------------------------/

    @Override
    public Job toSimpleData(HasMetadata hasMetadata) {
        Job job = (Job) hasMetadata;
        job.getMetadata().setResourceVersion(null);
        job.getMetadata().setSelfLink(null);
        job.getMetadata().setUid(null);
        job.getMetadata().setManagedFields(null);
        job.getMetadata().setOwnerReferences(null);

        return job;
    }

    @Override
    public List<Job> list() {
        log.info("get => list all");
        return k8sClientHolder.getClient().batch().jobs().inAnyNamespace().list().getItems();
    }

    @Override
    public List<Job> list(String namespace) {

        if (StringUtils.isBlank(namespace)) {
            return this.list();
        }
        log.info("get => namespace:{}", namespace);
        return k8sClientHolder.getClient().batch().jobs().inNamespace(namespace).list().getItems();
    }

    public List<Job> list(Long limit) {
        log.info("get => limit:{}", limit);
        return k8sClientHolder.getClient().batch().jobs().inAnyNamespace().list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    public List<Job> list(String namespace, Long limit) {

        // 若值为空或小于0则默认100条
        if (limit == null || limit < 0) {
            return this.list(namespace);
        }

        // 空间为空时，查询当前集群下所有信息
        if (StringUtils.isBlank(namespace)) {
            return this.list(limit);
        }
        log.info("get =>namespace:{} ,limit:{}", namespace, limit);
        return k8sClientHolder.getClient().batch().jobs().inNamespace(namespace).list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    public List<Job> get(String name) {
        log.info("get => name:{}", name);
        List<Job> result = new ArrayList<>();
        List<Namespace> items = k8sClientHolder.getClient().namespaces().list().getItems();
        if (CollectionUtils.isEmpty(items)) {
            return result;
        }

        for (Namespace item : items) {
            Job job = get(name, item.getMetadata().getName());
            if (job != null) {
                result.add(job);
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
    public Job get(String name, String namespace) {
        log.info("get => namespace:{} ,name:{}", namespace, name);
        return k8sClientHolder.getClient().batch().jobs().inNamespace(namespace).withName(name).get();
    }

    public List<Job> listByLabels(Map<String, String> labels){
        log.info("get => labels:{}", labels);
        return k8sClientHolder.getClient().batch().jobs().inAnyNamespace().withLabels(labels).list().getItems();
    }

    public List<Job> listByLabels(String namespace, Map<String, String> labels){
        log.info("get => namespace:{},labels:{}", namespace, labels);
        if (StringUtils.isEmpty(namespace)) {
            return this.listByLabels(labels);
        }
        return k8sClientHolder.getClient().batch().jobs().inNamespace(namespace).withLabels(labels).list().getItems();
    }

}
