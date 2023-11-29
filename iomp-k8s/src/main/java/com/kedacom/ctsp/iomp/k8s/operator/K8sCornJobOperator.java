package com.kedacom.ctsp.iomp.k8s.operator;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import com.kedacom.ctsp.iomp.k8s.common.cache.K8sAbstractOperator;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.batch.v1beta1.CronJob;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

@Slf4j
public class K8sCornJobOperator extends K8sAbstractOperator {

    public K8sCornJobOperator(K8sClientHolder k8sClientHolder) {
        super(k8sClientHolder);
    }

    public boolean createOrReplace(String name, String namespace, String yaml) {
        log.info("update => name:{},namespace:{},yaml:{}", name, namespace, yaml);
        try {
            DefaultKubernetesClient client = k8sClientHolder.getClient();
            CronJob cronJob = client.batch().cronjobs().inNamespace(namespace).withName(name).get();
            if (cronJob != null) {
                CronJob newCronJob = new Yaml().loadAs(yaml, CronJob.class);
                newCronJob.getMetadata().setResourceVersion(null);
                if (log.isDebugEnabled()) {
                    log.debug("newCronJob: " + JSON.toJSONString(newCronJob));
                }
                client.batch().cronjobs().inNamespace(namespace).createOrReplace(newCronJob);
                this.patchCache(newCronJob);
                return true;
            }
        } catch (Exception e) {
            log.error("patch k8sCronjob by yaml found exception, k8sCronjob:{}, namespace:{}, yaml:{}", name, namespace, yaml, e);
        }
        return false;
    }

    public boolean delete(String name, String namespace) {
        log.info("delete => name:{},namespace:{}", name, namespace);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        boolean result = client.batch().cronjobs().inNamespace(namespace).withName(name).delete();
        if(result){
            this.delCache(name,namespace);
        }
        return result;
    }

    public Boolean deleteWithLabelIn(String namespace,String key, String... values) {
        log.info("delete => namespace:{},key:{},values:{}", key, values);
        boolean result;
        if(StringUtils.isNotBlank(namespace)){
            result = k8sClientHolder.getClient().batch().cronjobs().inNamespace(namespace).withLabelIn(key, values).delete();
        }else{
            result = k8sClientHolder.getClient().batch().cronjobs().withLabelIn(key, values).delete();
        }
        if(result){
            this.delCacheByLabel(namespace,key,values);
        }
        return result;
    }



    //------------------------------------------------------------------------以下为重构方法--------------------------------/
    @Override
    public CronJob toSimpleData(HasMetadata hasMetadata) {
        CronJob cronJob = (CronJob) hasMetadata;
        cronJob.getMetadata().setResourceVersion(null);
        cronJob.getMetadata().setSelfLink(null);
        cronJob.getMetadata().setUid(null);
        cronJob.getMetadata().setManagedFields(null);
        cronJob.getMetadata().setOwnerReferences(null);

        cronJob.getSpec().setJobTemplate(null);
        return cronJob;
    }

    @Override
    public List<CronJob> list() {
        log.info("list all");
        return k8sClientHolder.getClient().batch().cronjobs().inAnyNamespace().list().getItems();
    }

    @Override
    public List<CronJob> list(String namespace) {

        if (StringUtils.isBlank(namespace)) {
            return this.list();
        }
        log.info("get => namespace:{}", namespace);
        return k8sClientHolder.getClient().batch().cronjobs().inNamespace(namespace).list().getItems();
    }

    public List<CronJob> list(Long limit){
        log.info("get => limit:{}", limit);
        return k8sClientHolder.getClient().batch().cronjobs().inAnyNamespace().list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    public List<CronJob> list(String namespace, Long limit) {

        // 若值为空或小于0则默认100条
        if (limit == null || limit < 0) {
            return this.list(namespace);
        }

        // 空间为空时，查询当前集群下所有信息
        if (StringUtils.isBlank(namespace)) {
            return this.list(limit);
        }
        log.info("get => namespace:{} ,limit:{}", namespace, limit);
        return k8sClientHolder.getClient().batch().cronjobs().inNamespace(namespace).list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    /**
     * 根据名称获取对象
     * @param namespace 必填，否则无法查询name
     * @param name
     * @return
     */
    public CronJob get(String name, String namespace) {
        log.info("get => namespace:{} ,name:{}", namespace, name);
        return k8sClientHolder.getClient().batch().cronjobs().inNamespace(namespace).withName(name).get();
    }


    public  List<CronJob> listByLabels(Map<String, String> labels){
        log.info("get => labels:{}", labels);
        return k8sClientHolder.getClient().batch().cronjobs().inAnyNamespace().withLabels(labels).list().getItems();
    }

    public List<CronJob> listByLabels(String namespace, Map<String, String> labels){
        log.info("get => namespace:{},labels:{}", namespace, labels);
        if (StringUtils.isEmpty(namespace)) {
            return this.listByLabels(labels);
        }
        return k8sClientHolder.getClient().batch().cronjobs().inNamespace(namespace).withLabels(labels).list().getItems();
    }

    public List<CronJob>  listByLabels(List<String> labels) {
        log.info("get => labels:{}", labels);
        List<LabelSelectorRequirement> requirements = Lists.newArrayList();
        for (String label : labels) {
            requirements.add(new LabelSelectorRequirementBuilder()
                    .withKey(label).withOperator("Exists").build());
        }
        return k8sClientHolder.getClient().batch().cronjobs().inAnyNamespace().withLabelSelector(new LabelSelectorBuilder().withMatchExpressions(requirements).build()).list().getItems();
    }

    public List<CronJob> listByLabels(String namespace, List<String> labels) {
        if (StringUtils.isBlank(namespace)) {
            return this.listByLabels(labels);
        }
        List<LabelSelectorRequirement> requirements = Lists.newArrayList();
        for (String label : labels) {
            requirements.add(new LabelSelectorRequirementBuilder()
                    .withKey(label).withOperator("Exists").build());
        }
        log.info("get => namespace:{},labels:{},requirements", namespace, labels, requirements);
        return k8sClientHolder.getClient().batch().cronjobs().inNamespace(namespace).withLabelSelector(new LabelSelectorBuilder().withMatchExpressions(requirements).build()).list().getItems();
    }

    public List<CronJob> listByLabel(String namespace, String label, String... value) {
        if (StringUtils.isBlank(namespace)) {
            return this.listByLabelCustom(label,value);
        }
        log.info("get => namespace:{} ,label:{},value{}", namespace, value);
        return k8sClientHolder.getClient().batch().cronjobs().inNamespace(namespace).withLabelIn(label, value).list().getItems();

    }

    public List<CronJob> listByLabelCustom(String label, String... value){
        log.info("get => label:{},value{}", label, value);
        return k8sClientHolder.getClient().batch().cronjobs().inAnyNamespace().withLabelIn(label, value).list().getItems();
    }
}
