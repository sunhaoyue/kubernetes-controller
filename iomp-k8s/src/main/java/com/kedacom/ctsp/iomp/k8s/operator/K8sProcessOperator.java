package com.kedacom.ctsp.iomp.k8s.operator;

import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;

import java.util.List;

public class K8sProcessOperator {

    private final K8sClientHolder k8sClientHolder;

    public K8sProcessOperator(K8sClientHolder k8sClientHolder) {
        this.k8sClientHolder = k8sClientHolder;
    }


    /**
     * 获取deployment
     */
    public List<Deployment> getAllDeployment(){
        return k8sClientHolder.getClient().apps().deployments().inAnyNamespace().list().getItems();
    }


    /**
     * 获取statufulSet
     */
    public List<StatefulSet> getAllStatufulSet(){
        return k8sClientHolder.getClient().apps().statefulSets().inAnyNamespace().list().getItems();
    }


    /**
     * 获取statufulSet
     */
    public List<DaemonSet> getAllDaemonSet(){
        return k8sClientHolder.getClient().apps().daemonSets().inAnyNamespace().list().getItems();
    }


    public PersistentVolumeClaim getPvc(String pvcName,String namespace){
        return  k8sClientHolder.getClient().persistentVolumeClaims().inNamespace(namespace).withName(pvcName).get();
    }
}
