package com.kedacom.ctsp.iomp.k8s.constant;

/**
 * K8s常量类
 *
 * @author HeFei
 * @create 2018-05-25 16:27
 **/
public class K8sConstants {

    /**
     * 镜像拉取策略： 不管镜像是否存在都会进行一次拉取
     */
    public static final String IMAGE_PULL_POLICY_ALWAYS = "Always";
    /**
     * 镜像拉取策略： 不管镜像是否存在都不会进行拉取
     */
    public static final String IMAGE_PULL_POLICY_NEVER = "Never";
    /**
     * 镜像拉取策略： 只有镜像不存在时，才会进行镜像拉取
     */
    public static final String IMAGE_PULL_POLICY_RESENT = "IfNotPresent";

    public static final String IP_ADDRS_NO_IPAM = "cni.projectcalico.org/ipAddrsNoIpam";

    /**
     * ip池注解
     */
    public static final String IPV4_POOLS = "cni.projectcalico.org/ipv4pools";

    /**
     * pod出现故障时的操作： 创建新的pod，且故障pod不会消失。.status.failed加1。
     */
    public static final String RESTART_POLICY_NEVER = "Never";
    /**
     * pod出现故障时的操作： 重启容器，而不是创建pod。.status.failed不变。
     */
    public static final String RESTART_POLICY_ON_FAILURE = "OnFailure";
    public static final String RESTART_POLICY_ALWAYS = "Always";

    public static final String CLUSTER_FIRST = "ClusterFirst";

    public static final String CLUSTER_IP = "ClusterIP";


    public static final String JOB_NAME_PS = "ps";
    public static final String JOB_NAME_WORKER = "worker";

    public static final String NAME = "name";

    public static final String JOB_NAME = "job_name";
    public static final String TASK_ID = "task_id";
    public static final String PS_HOSTS = "ps_hosts";
    public static final String WORKER_HOSTS = "worker_hosts";
    public static final String NUM_GPUS = "num_gpus";

    public static final String TASK_INDEX = "task_index";
    public static final String WORK_SPACE = "workspace";
    public static final String CUDA = "cuda";
    public static final String NVIDIA = "nvidia";
    public static final String API_VERSION_JOB = "batch/v1";
    public static final String API_VERSION_DEPLOYMENT = "apps/v1";
    public static final String API_VERSION_V1BETA1 = "extensions/v1beta1";
    public static final String API_VERSION_V1 = "v1";
    public static final String API_VERSION_CLUSTER_ROLE = "rbac.authorization.k8s.io/v1";
    public static final String API_GROUP_CLUSTER_ROLE = "rbac.authorization.k8s.io";
    public static final String API_VERSION_STORAGE = "storage.k8s.io/v1";
    public static final String LABEL_NAME = "name";
    public static final String NVIDIA_GPU = "alpha.kubernetes.io/nvidia-gpu";
    public static final String TENSORBOARD = "tensorboard";
    public static final String TENSORBOARD_TYPE_NODE = "NodePort";
    public static final String TENSORBOARD_TYPE_CLUSTER = "ClusterIP";

    public static final String KIND_DEPLOYMENT = "Deployment";
    public static final String KIND_JOB = "Job";

    public static final String INGRESS_SSL_REDIRECT = "nginx.ingress.kubernetes.io/ssl-redirect";
    public static final String INGRESS_AFFINITY = "nginx.ingress.kubernetes.io/affinity";
    public static final String INGRESS_COOKIE_NAME = "nginx.ingress.kubernetes.io/session-cookie-name";
    public static final String INGRESS_COOKIE_HASH = "nginx.ingress.kubernetes.io/session-cookie-hash";
    public static final String INGRESS_PROXY_BODY_SIZE = "nginx.ingress.kubernetes.io/proxy-body-size";
    public static final String INGRESS_REWRITE_TARGET = "nginx.ingress.kubernetes.io/rewrite-target";
    public static final String INGRESS_CONFIGURATION_SNIPPET = "nginx.ingress.kubernetes.io/configuration-snippet";

    public static final String INGRESS_CONFIGURATION_KONGHQ = "configuration.konghq.com";

    public static final String ACCESS_MODES_RWM = "ReadWriteMany";
    public static final String ACCESS_MODES_RWO = "ReadWriteOnce";
    public static final String STORAGE = "storage";
    public static final String RECYCLE = "Recycle";
    public static final String RETAIN = "Retain";

    public static final String TYPE = "type";
    public static final String LOCAL = "local";

    public static final String VOLUME_MODE_FILE = "Filesystem";

    public static final String ENV_NFS_PROVISIONER_NAME = "PROVISIONER_NAME";
    public static final String ENV_NFS_SERVER = "NFS_SERVER";
    public static final String ENV_NFS_PATH = "NFS_PATH";


    public static final String STATUS_RUNNING = "Running";
    public static final String STATUS_READY = "Ready";
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_TERMINATING = "Terminating";
    public static final String STATUS_INIT = "Init:";


    public static final String NFS_BASE_IMAGE = "nfs-client-provisioner:v1.0.0";

    public static final String TOMCAT_BASE_IMAGE = "dolphin/tomcat_oraclejdk:8.5.27";
    public static final String TOMCAT_BASE_OLD_IMAGE = "dolphin/tomcat:8.5.27";
    public static final String JDK_BASE_IMAGE = "dolphin/jdk:1.8";
    public static final String NGINX_BASE_IMAGE = "dolphin/nginx:1.13";
    public static final String FLUME_BASE_IMAGE = "dolphin/flume:1.8";
    public static final String NODE_BASE_IMAGE = "dolphin/node:8.9.3";
    public static final String GCC_BASE_IMAGE = "dolphin/gcc_common_run:latest";

    public static final String TOMCAT_PKGNAME = "app.war";
    public static final String JDK_PKGNAME = "app.jar";
    public static final String NGINX_PKGNAME = "dist.zip";
    public static final String FLUME_PKGNAME = "project.zip";
    public static final String NODE_PKGNAME = "node.tar.gz";
    public static final String GCC_PKGNAME = "project.tar.gz";

    public static final String GLOBAL_CONFIGMAP_NAME = "global-config";

    public static final String K8S_DEPLOYMENT = "deployment";
    public static final String K8S_STATEFULSET = "statefulset";
    public static final String K8S_DAEMONSET = "daemonset";

    public static final String NS_DEFAULT = "default";
    public static final String NS_KEDACOM_PROJECT_NAMESPACE = "kedacom-project-namespace";


    /*
    namespace annotation 上存储域名证书和网关
    多个以逗号  隔开
     */
    public static final String NS_DOMAINS = "dophin/domains";
    public static final String NS_GATEWAY = "dophin/gateways";
    public static final String NS_TCP_GATEWAY = "dophin/tcp-gateways";
    public static final String NS_STORAGE = "dophin/storage";
    public static final String NS_REGIONS = "dolphin/regions";

    public static final String NS_TIMEZONE = "dolphin/timezone";
    public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    public static final String INGRESS_CLASS = "kubernetes.io/ingress.class";

    /**
     * 空间属性V2
     */
    public static final String ANN_ATTRIBUTES = "dolphin/attributes";

    public static final String ENABLED = "enabled";

    public static final String DISABLED = "disabled";


    /**
     * 本地存储标签和注解常量
     */
    public static final String SC_CREATE_USER = "dolphin.storage/createUser";
    public static final String SC_SHARE_MODE = "dolphin.storage/shareMode";
    public static final String SC_TYPE = "dolphin.storage/type";
    public static final String SC_CATEGORY = "dolphin.storage/category";
    public static final String SC_DISK = "dolphin.storage/disk";

    public static final String SC_LOCAL_PROVISIONER = "kubernetes.io/no-provisioner";

    public static final String LOCAL_PV_CONFIG = "localpv-relation-config";

    public static final String CLUSTER_VERSION_CONFIGMAP = "cluster-version-configmap";

    public static final String CLUSTER_INFO = "cluster-info";
    public static final String K3S_SINGLE_IP = "k3sIp";


    /**
     *
     */
    public static final String ALWAYS = "Always";
    /**
     *
     */
    public static final String NEVER = "Never";

}
