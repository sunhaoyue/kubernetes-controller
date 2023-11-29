package com.kedacom.ctsp.iomp.k8s.enmu;

/**
 * @ClassName
 * @Description k8s 资源类型 枚举
 * @Author sunhaoyue
 * @Date 2021/5/18 20:52
 * @Version 1.0
 **/
public enum K8sResourceTypeEnum {
    Deployment,
    StatefulSet,
    DaemonSet,
    Pod,
    Service,
    Ingress,
    ConfigMap,
    Secret,
    CronJob,
    Job,
    PVC,
    PV,
    Namespace
}
