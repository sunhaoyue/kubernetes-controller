package com.kedacom.ctsp.iomp.k8s.operator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import com.kedacom.ctsp.iomp.k8s.Net7Protocol;
import com.kedacom.ctsp.iomp.k8s.NodeNullPointerException;
import com.kedacom.ctsp.iomp.k8s.config.IngressConfig;
import com.kedacom.ctsp.iomp.k8s.constant.K8sConstants;
import com.kedacom.ctsp.iomp.k8s.constant.K8sKindConstant;
import com.kedacom.ctsp.iomp.k8s.constant.K8sLabels;
import com.kedacom.ctsp.iomp.k8s.enmu.K8sResourceTypeEnum;
import com.kedacom.ctsp.iomp.k8s.model.Callback;
import com.kedacom.ctsp.iomp.k8s.vo.*;
import com.kedacom.ctsp.lang.exception.CommonException;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.DaemonSetList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.api.model.apps.*;
import io.fabric8.kubernetes.api.model.extensions.*;
import io.fabric8.kubernetes.api.model.rbac.*;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.fabric8.kubernetes.api.model.storage.StorageClassBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.kubernetes.client.dsl.ScalableResource;
import io.fabric8.kubernetes.client.utils.InputStreamPumper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author Administrator
 */
@Slf4j
@Data
public class K8sDeployOperator {

    public static final String K8S_NAME_REG = "^[a-z0-9]([-a-z0-9]*[a-z0-9])?$";
    public static final String NFS_NAME_PREX = "nfs-";

    private static final String SSL_REDIRECT = "nginx.ingress.kubernetes.io/ssl-redirect";
    private static final String AFFINITY = "nginx.ingress.kubernetes.io/affinity";
    private static final String SESSION_COOKIE_NAME = "nginx.ingress.kubernetes.io/session-cookie-name";
    private static final String SESSION_COOKIE_HASH = "nginx.ingress.kubernetes.io/session-cookie-hash";
    private static final String WEB_SOCKET = "nginx.org/websocket-services";
    private static final String CONFIGURATION_KONG_INGRESS = "configuration.konghq.com";
    private static final String CONFIGURATION_KONG_PLUGIN = "plugins.konghq.com";
    private static final String PROJECT_MARK = "projectMark";

    private static final String SESSION_COOKIE_PATH = "nginx.ingress.kubernetes.io/session-cookie-path";
    private static final String USE_REGEX = "nginx.ingress.kubernetes.io/use-regex";
    private static final String REWRITE_TARGET = "nginx.ingress.kubernetes.io/rewrite-target";
    private static final String NGINX_CONFIG_SNIPPET = "nginx.ingress.kubernetes.io/configuration-snippet";
    private static final String LABEL_INSTANCE = "app.kubernetes.io/instance";
    private static final String DOLPHIN_CLIENT_ID = "dolphin/client_id";

    /**
     * 最大等待pods时间 10分钟 按照毫秒值
     */
    private static final int MAX_WAIT_POD_TIMEOUT_MS = 10 * 60 * 1000;

    /**
     * 最大等待pods时间 5分钟 按照毫秒值
     */
    private static final int MAX_WAIT_EXEC_TIMEOUT_MS = 5 * 60 * 1000;

    /**
     * 在容器内执行命令的结束标识
     */
    private static final String EXEC_END_FLAG = "exec-shell-end-flag";

    /**
     * 在容器内执行命令的结束标识的命令
     */
    private static final String EXEC_END_COMMAND = ";echo ".concat(EXEC_END_FLAG);


    private final K8sClientHolder k8sClientHolder;

    /**
     * 类型
     */
    private K8sResourceTypeEnum kind;

    private IngressConfig ingressConfig;

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public K8sDeployOperator(K8sClientHolder k8sClientHolder) {
        this.k8sClientHolder = k8sClientHolder;
    }

    public void list() {
        log.info("刷新数据==========>list all type");
        new K8sConfigmapOperator(k8sClientHolder).refreshCache();
//        new K8sCornJobOperator(k8sClientHolder).refreshCache();
        new K8sDaemonSetOperator(k8sClientHolder).refreshCache();
        new K8sDeploymentOperator(k8sClientHolder).refreshCache();
        new K8sIngressOperator(k8sClientHolder).refreshCache();
        new K8sJobOperator(k8sClientHolder).refreshCache();
//        new K8sNamespaceOperator(k8sClientHolder).refreshCache();
//        new K8sNodeOperator(k8sClientHolder).refreshCache();
        new K8sPodOperator(k8sClientHolder).refreshCache();
//        new K8sPriorityClassOperator(k8sClientHolder).refreshCache();
        new K8sPvcOperator(k8sClientHolder).refreshCache();
//        new K8sPvOperator(k8sClientHolder).refreshCache();
        new K8sSecretOperator(k8sClientHolder).refreshCache();
        new K8sServiceOperator(k8sClientHolder).refreshCache();
        new K8sStatefulSetOperator(k8sClientHolder).refreshCache();
//        new K8sStorageClassOperator(k8sClientHolder).refreshCache();

    }

    /**
     * 获取k8s版本信息
     *
     * @return
     */
    public VersionInfo getVersionInfo() {
        return k8sClientHolder.getClient().getVersion();
    }


    public String getVersion(int timeout) {
        log.info("get => timeout:{}", timeout);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        int oldTimeOut = client.getConfiguration().getRequestTimeout();
        try {
            client.getConfiguration().setRequestTimeout(timeout);
            return client.getVersion().getGitVersion();
        } finally {
            client.getConfiguration().setRequestTimeout(oldTimeOut);
        }
    }

    public void setScale(String deployName, String namespace, int scale) {
        log.info("update => deployName:{},namespace:{},scale:{}", deployName, namespace, scale);
        try {
            Deployment deployment = getDeployment(deployName, namespace);
            if (deployment != null) {
                deployment.getSpec().setReplicas(scale);
                new K8sDeploymentOperator(this.k8sClientHolder).createOrReplace(namespace, deployment);
//                k8sClientHolder.getClient().apps().deployments().inNamespace(namespace).createOrReplace(deployment);
                return;
            }

            StatefulSet sf = getStatefulSet(deployName, namespace);
            if (sf != null) {
                sf.getSpec().setReplicas(scale);
                new K8sStatefulSetOperator(this.k8sClientHolder).createOrReplace(namespace, sf);
//                k8sClientHolder.getClient().apps().statefulSets().inNamespace(namespace).createOrReplace(sf);
                return;
            }
        } catch (Exception e) {
            log.error("set sacle error:", e);
        }
        return;

    }


    public void createIngress(String namespace, Ingress ingress) {
        log.info("update => namespace:{},ingress:{}", namespace, ingress);
        try {
            new K8sIngressOperator(this.k8sClientHolder).createOrReplace(namespace, ingress);
//            k8sClientHolder.getClient().extensions().ingresses().inNamespace(namespace).createOrReplace(ingress);
        } catch (Exception e) {
            log.error("create ingress error:", e);
        }
    }

    public void getService(String namespace) {
        log.info("get => namespace:{}", namespace);
        try {
            ServiceList list = k8sClientHolder.getClient().services().inNamespace(namespace).list();
            log.info("ServiceList:" + new ObjectMapper().writeValueAsString(list));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        try {
            EndpointsList list = k8sClientHolder.getClient().endpoints().inNamespace(namespace).list();
            log.info("EndpointsList:" + new ObjectMapper().writeValueAsString(list));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        try {
            ServiceAccountList list = k8sClientHolder.getClient().serviceAccounts().inNamespace(namespace).list();
            log.info("ServiceAccountList:" + new ObjectMapper().writeValueAsString(list));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }

    /**
     * 创建 serviceAccount
     *
     * @param name
     * @param namespace
     */
    public void createServiceAccount(String name, String namespace) {
        log.info("update => name:{},namespace:{}", name, namespace);
        if (k8sClientHolder.getClient().serviceAccounts().inNamespace(namespace).withName(name) == null
                || k8sClientHolder.getClient().serviceAccounts().inNamespace(namespace).withName(name).get() == null) {
            ServiceAccountBuilder sab = new ServiceAccountBuilder();
            sab.withApiVersion("v1").withNewMetadata().withName(name).withNamespace(namespace).endMetadata();

            k8sClientHolder.getClient().serviceAccounts().inNamespace(namespace).createOrReplace(sab.build());
        }
    }

    /**
     * 删除 serviceAccount
     *
     * @param name
     * @param
     */
    public Boolean deleteServiceAccount(String name, String nameSpace) {
        log.info("delete => name:{},namespace:{}", name, nameSpace);
        Boolean res = true;
        if (k8sClientHolder.getClient().serviceAccounts().inNamespace(nameSpace).withName(name) != null
                && k8sClientHolder.getClient().serviceAccounts().inNamespace(nameSpace).withName(name).get() != null) {
            res = k8sClientHolder.getClient().serviceAccounts().inNamespace(nameSpace).withName(name).delete();
        }
        return res;
    }

    /**
     * 删除 ClusterRole
     *
     * @param name
     * @param
     */
    public Boolean deleteClusterRole(String name) {
        log.info("delete => name:{}", name);
        Boolean res = true;
        if (k8sClientHolder.getClient().rbac().clusterRoles().withName(name) != null
                && k8sClientHolder.getClient().rbac().clusterRoles().withName(name).get() != null) {
            res = k8sClientHolder.getClient().rbac().clusterRoles().withName(name).delete();
        }
        return res;
    }

    /**
     * 删除 createClusterRoleBinding
     *
     * @param name
     * @param
     */
    public Boolean deleteClusterRoleBinding(String name) {
        log.info("delete => name:{}", name);
        Boolean res = true;
        if (k8sClientHolder.getClient().rbac().clusterRoleBindings().withName(name) != null
                && k8sClientHolder.getClient().rbac().clusterRoleBindings().withName(name).get() != null) {
            res = k8sClientHolder.getClient().rbac().clusterRoleBindings().withName(name).delete();
        }
        return res;
    }

    /**
     * 创建 ClusterRole
     *
     * @param name
     * @param namespace
     */
    public void createClusterRole(String name, String namespace) {
        log.info("update => name:{},namespace:{}", name, namespace);
        if (k8sClientHolder.getClient().rbac().clusterRoles().withName(name) == null
                || k8sClientHolder.getClient().rbac().clusterRoles().withName(name).get() == null) {
            List<PolicyRule> rules = new ArrayList<>();
            rules.add(new PolicyRule(Lists.newArrayList(StringUtils.EMPTY), null, null,
                    Lists.newArrayList("persistentvolumes"), Lists.newArrayList("get", "list", "watch", "create", "delete")));
            rules.add(new PolicyRule(Lists.newArrayList(StringUtils.EMPTY), null, null,
                    Lists.newArrayList("persistentvolumeclaims"), Lists.newArrayList("get", "list", "watch", "update")));
            rules.add(new PolicyRule(Lists.newArrayList("storage.k8s.io"), null, null,
                    Lists.newArrayList("storageclasses"), Lists.newArrayList("get", "list", "watch")));
            rules.add(new PolicyRule(Lists.newArrayList(StringUtils.EMPTY), null, null,
                    Lists.newArrayList("events"), Lists.newArrayList("watch", "create", "update", "patch")));
            rules.add(new PolicyRule(Lists.newArrayList(StringUtils.EMPTY), null, null,
                    Lists.newArrayList("services", "endpoints"), Lists.newArrayList("get")));
            rules.add(new PolicyRule(Lists.newArrayList("extensions"), null, Lists.newArrayList("nfs-provisioner"),
                    Lists.newArrayList("podsecuritypolicies"), Lists.newArrayList("use")));

            ClusterRoleBuilder builder = new ClusterRoleBuilder();
            builder.withApiVersion(K8sConstants.API_VERSION_CLUSTER_ROLE)
                    .withNewMetadata().withName(name).withNamespace(namespace).endMetadata()
                    .withRules(rules);
            if (log.isDebugEnabled()) {
                log.info("clusterRole: " + JSON.toJSONString(builder.build()));
            }
            k8sClientHolder.getClient().inNamespace(namespace).rbac().clusterRoles().createOrReplace(builder.build());
        }
    }

    /**
     * 创建 ClusterRoleBinding
     *
     * @param name
     * @param namespace
     */
    public void createClusterRoleBinding(String name, String namespace) {
        log.info("update => name:{},namespace:{}", name, namespace);
        if (k8sClientHolder.getClient().rbac().clusterRoleBindings().withName(name) == null
                || k8sClientHolder.getClient().rbac().clusterRoleBindings().withName(name).get() == null) {
            ClusterRoleBindingBuilder builder = new ClusterRoleBindingBuilder();
            builder.withApiVersion(K8sConstants.API_VERSION_CLUSTER_ROLE)
                    .withNewMetadata().withName(name).withNamespace(namespace).endMetadata()
                    .withSubjects(new Subject(null, "ServiceAccount", name, namespace))
                    .withRoleRef(new RoleRef(K8sConstants.API_GROUP_CLUSTER_ROLE, "ClusterRole", name));
            if (log.isDebugEnabled()) {
                log.info("ClusterRoleBinding: " + JSON.toJSONString(builder.build()));
            }
            k8sClientHolder.getClient().inNamespace(namespace).rbac().clusterRoleBindings().createOrReplace(builder.build());
        }
    }

    /**
     * 创建 Nfs-Provisioner
     * 添加外部存储，需要自己创建 serviceAccount，clusterRole
     *
     * @param vo
     */
    public void createNfsProvisioner(K8sNfsProvisionerBuildVo vo) {
        log.info("update => K8sNfsProvisionerBuildVo:{}", JSON.toJSON(vo));
//         1. 创建 serviceAccount
        createServiceAccount(vo.getName(), vo.getNameSpace());
//         2. 创建 ClusterRole
        createClusterRole(vo.getName(), vo.getNameSpace());
//         3. 创建 ClusterRoleBindings
        createClusterRoleBinding(vo.getName(), vo.getNameSpace());

        Map<String, String> labels = new HashMap<>();
        labels.put("app", vo.getName());

        VolumeMount vm = new VolumeMount();
        vm.setName(vo.getMountName());
        vm.setMountPath(vo.getMountPath());

        List<VolumeMount> volumeMounts = new ArrayList<>();
        volumeMounts.add(vm);

        EnvVar env = new EnvVar();
        env.setName(K8sConstants.ENV_NFS_PROVISIONER_NAME);
        env.setValue(vo.getName());

        EnvVar nfsServerEnv = new EnvVar();
        nfsServerEnv.setName(K8sConstants.ENV_NFS_SERVER);
        nfsServerEnv.setValue(vo.getNfsServer());

        EnvVar nfsPathEnv = new EnvVar();
        nfsPathEnv.setName(K8sConstants.ENV_NFS_PATH);
        nfsPathEnv.setValue(vo.getNfsPath());

        List<EnvVar> envList = new ArrayList<>();
        envList.add(env);
        envList.add(nfsServerEnv);
        envList.add(nfsPathEnv);

        Container containers = new Container();
        containers.setName(vo.getName());
        containers.setImage(vo.getImage());
        containers.setImagePullPolicy(K8sConstants.IMAGE_PULL_POLICY_RESENT);
        containers.setVolumeMounts(volumeMounts);
        containers.setEnv(envList);
        Map<String, Quantity> requests = Maps.newHashMap();
        requests.put("cpu", new Quantity("0", null));
        requests.put("memory", new Quantity("0", "Mi"));
        Map<String, Quantity> limits = Maps.newHashMap();
        limits.put("cpu", new Quantity("64", "m"));
        limits.put("memory", new Quantity("128", "Mi"));
        containers.setResources(new ResourceRequirementsBuilder().withRequests(requests).withLimits(limits).build());

        NFSVolumeSource nfs = new NFSVolumeSource();
        nfs.setServer(vo.getNfsServer());
        nfs.setPath(vo.getNfsPath());

        Volume volumes = new Volume();
        volumes.setName(vo.getMountName());
        volumes.setNfs(nfs);

        Map<String, String> annotations = Maps.newHashMap();
        annotations.put("deployment.kubernetes.io/revision", "1");
        annotations.put("hostIps", vo.getNfsServer());
        annotations.put("cephPath", vo.getNfsPath());
        Map<String, String> provisionerLabels = Maps.newHashMap();
        provisionerLabels.put("app.kubernetes.io/instance", vo.getName());
        provisionerLabels.put("app.kubernetes.io/managed-by", "iomp-api");
        provisionerLabels.put("app.kubernetes.io/name", "nfs-client");

        DeploymentBuilder deploymentBuilder = new DeploymentBuilder();
        deploymentBuilder
                .withApiVersion(K8sConstants.API_VERSION_DEPLOYMENT)
                .withNewMetadata()
                .withName(vo.getName())
                .withNamespace(vo.getNameSpace())
                .withLabels(provisionerLabels)
                .withAnnotations(annotations)
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .withMatchLabels(labels).endSelector()
                .withReplicas(vo.getReplicas())
                .withNewStrategy().withType("Recreate").endStrategy()
                .withNewTemplate()
                .withNewMetadata().withLabels(labels).endMetadata()
                .withNewSpec()
                .withServiceAccount("admin")
                .withContainers(containers)
                .withVolumes(volumes)
                //.withTolerations(new TolerationBuilder().withOperator("Exists").build())
                .endSpec()
                .endTemplate()
                .endSpec();
        Deployment provisioner = deploymentBuilder.build();
        Long startTime = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.info("provisioner: " + JSON.toJSONString(provisioner));
        }

        k8sClientHolder.getClient().apps().deployments().inNamespace(vo.getNameSpace()).createOrReplace(provisioner);

    }

    /**
     * 删除 Nfs-Provisioner
     *
     * @param
     */
    public void deleteNfsProvisioner(String name, String nameSpace) {
        log.info("delete => name:{},namespace:{}", name, nameSpace);
        // 1. 删除 serviceAccount
        deleteServiceAccount(name, nameSpace);
        // 2. 删除 ClusterRole
        deleteClusterRole(name);
        // 3. 删除 ClusterRoleBindings
        deleteClusterRoleBinding(name);
        // 4. 删除 deployments
        deleteDeployment(name, nameSpace);
    }

    public boolean getDeploymentsStatus(String name, String nameSpace) {
        log.info("get => name:{},namespace:{}", name, nameSpace);
        ScalableResource<Deployment> deploymentResource =
                k8sClientHolder.getClient().apps().deployments().inNamespace(nameSpace).withName(name);
        if (deploymentResource == null) {
            return false;
        }
        Deployment deployment = deploymentResource.get();
        if (deployment == null) {
            return false;
        }

        String status = deployment.getStatus().getConditions().get(0).getStatus();

        if (StringUtils.equalsIgnoreCase(status, "true")) {
            return true;
        }
        return false;
    }

    /**
     * 创建 StorageClass
     *
     * @param vo
     */
    public void createStorageClass(K8sStorageClassVo vo) {
        log.info("update => K8sNfsProvisionerBuildVo:{}", JSON.toJSON(vo));
        StorageClassBuilder scb = new StorageClassBuilder();
        scb.withApiVersion(K8sConstants.API_VERSION_STORAGE)
                .withNewMetadata()
                .withName(vo.getName())
                .withAnnotations(vo.getAnnotations())
                .withNamespace(vo.getNameSpace()).endMetadata()
                .withProvisioner(vo.getProvisioner())
                .withReclaimPolicy(K8sConstants.RETAIN);
        Set<String> mountOptionSet = Sets.newHashSet();
        if (StringUtils.isNotEmpty(vo.getMountOptions())) {
            mountOptionSet = Sets.newHashSet(StringUtils.split(vo.getMountOptions(), ","));
        }
        if (!BooleanUtils.isFalse(vo.getNfsLock())) {
            mountOptionSet.add("nolock");
//            scb.addNewMountOption("nolock");
        }
        scb.addAllToMountOptions(mountOptionSet);
        StorageClass storageClass = scb.build();
//        k8sClientHolder.getClient().storage().storageClasses().createOrReplace(storageClass);
        new K8sStorageClassOperator(this.k8sClientHolder).createOrReplace(storageClass);
    }

    /**
     * 创建 StorageClass
     */
    public boolean deleteStorageClass(String name) {
        log.info("delete => name:{}", name);
        boolean res = true;
        if (k8sClientHolder.getClient().storage().storageClasses().withName(name) != null
                && k8sClientHolder.getClient().storage().storageClasses().withName(name).get() != null) {

            res = new K8sStorageClassOperator(this.k8sClientHolder).delete(name);
//            res = k8sClientHolder.getClient().storage().storageClasses().withName(name).delete();
        }
        // 判断是否存在
        return res;
    }

    /**
     * 创建pvc
     *
     * @param vo
     */
    public void createPvc(K8sPvcVo vo) {
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
        pb.withApiVersion(K8sConstants.API_VERSION_V1)
                .withNewMetadata()
                .withName(vo.getName())
                .withNamespace(vo.getNamespace())
                .withLabels(labels)
                .endMetadata()
                .withNewSpec()
                .withAccessModes(K8sConstants.ACCESS_MODES_RWM)
                .withNewResources()
                .withRequests(limits).endResources()
                .withStorageClassName(storageClassName).endSpec();
        new K8sPvcOperator(this.k8sClientHolder).createOrReplace(pb.build());
//        k8sClientHolder.getClient().persistentVolumeClaims().createOrReplace(pb.build());
    }

    /**
     * 获取集群节点
     *
     * @return
     */
    public List<Node> getNodeList() {
        return k8sClientHolder.getClient().nodes().list().getItems();
    }

    /**
     * 删除节点
     *
     * @param nodeName
     * @return
     */
    public Boolean deleteNodeByNodeName(String nodeName) {
        log.info("get => nodeName:{}", nodeName);
        Boolean res = true;
        // 节点存在的话删除节点
        if (k8sClientHolder.getClient().nodes().withName(nodeName) != null) {
            res = new K8sNodeOperator(this.k8sClientHolder).delete(nodeName);
//            res = k8sClientHolder.getClient().nodes().withName(nodeName).delete();
        }
        return res;
    }


    /**
     * 获取所有configMap
     *
     * @return
     */
    public List<ConfigMap> getConfigMapList() {
        log.info("get => list all");
        return k8sClientHolder.getClient().configMaps().list().getItems();
    }

    public Map<String, String> getConfigMapByName(String mapName) {
        log.info("get => mapName:{}", mapName);
        Map<String, String> configInfo = new HashMap<>();
        List<ConfigMap> items = getConfigMapList();
        for (ConfigMap configMap : items) {
            if (StringUtils.equals(mapName, configMap.getMetadata().getName())) {
                configInfo = configMap.getData();
            }
        }

        return configInfo;
    }

    /**
     * 根据map名获取对应的map
     *
     * @param mapName
     * @return
     */
    public Map<String, String> getConfigMapByName(String mapName, String namespace) {
        log.info("get => mapName:{},namespace:{}", mapName, namespace);
        ConfigMap configMap = k8sClientHolder.getClient().configMaps().inNamespace(namespace).withName(mapName).get();
        if (configMap == null) {
            return null;
        }
        return configMap.getData();

    }

    /**
     * 获取节点
     *
     * @param nodeName
     * @return
     */
    public Node getNodeByNodeName(String nodeName) {
        log.info("get => nodeName:{}", nodeName);
        if (k8sClientHolder.getClient().nodes().withName(nodeName) != null) {
            return k8sClientHolder.getClient().nodes().withName(nodeName).get();
        } else {
            return null;
        }
    }


    /**
     * 创建tomcat 的 ingress
     *
     * @param ingressVo
     * @return
     */
    public Ingress createTomcatIngress(IngressVo ingressVo) {
        if (StringUtils.isBlank(ingressVo.getPath())) {
            ingressVo.setPath("/" + ingressVo.getIngressName());
        } else {
            if (!ingressVo.getPath().startsWith("/")) {
                ingressVo.setPath("/" + ingressVo.getPath());
            }
        }
        return createDefaultIngress(ingressVo);
    }

    public Ingress createDefaultIngress(IngressVo ingressVo) {
        log.info("update => ingressVo:{}", JSON.toJSONString(ingressVo));
        String ingressName = ingressVo.getIngressName() + "-default";

        Map<String, String> labels = new HashMap<>();
        labels.put("name", ingressVo.getIngressName());

        IngressBuilder builder = new IngressBuilder()
                .withApiVersion(K8sConstants.API_VERSION_V1BETA1)
                .withNewMetadata()
                .withLabels(labels)
                .withName(ingressName)
                .withNamespace(ingressVo.getNameSpace())
                .withAnnotations(new HashMap<String, String>() {
                    private static final long serialVersionUID = -1055767662489940859L;

                    {
                        put(K8sConstants.INGRESS_CONFIGURATION_KONGHQ, "kong-ingress-common-https");
                    }
                }).endMetadata()
                .withNewSpec()
                .addNewRule()
                .withNewHttp()
                .addNewPath()
                .withPath(ingressVo.getPath())
                .withNewBackend()
                .withServiceName(ingressVo.getIngressName())
                .withServicePort(new IntOrString(ingressVo.getIngressPort()))
                .endBackend()
                .endPath()
                .endHttp()
                .endRule()
                .endSpec();
        log.info("IngressBuilder:{}", JSON.toJSONString(builder.build()));
//        k8sClientHolder.getClient().extensions().ingresses().createOrReplace(builder.build());
        new K8sIngressOperator(this.k8sClientHolder).createOrReplace(builder.build());
        return builder.build();

    }

    /**
     * 根据路由规则创建ingress
     *
     * @param routeConfig
     * @return
     */
    public void createRouteIngress(K8sRouteConfigVo routeConfig, Boolean deleteFlag, String nameSpace) {
        log.info("get => routeConfig:{},deleteFlag:{},nameSpace{}", routeConfig, deleteFlag, nameSpace);
        if (routeConfig.getStripPath() == null) {
            routeConfig.setStripPath(0);
        }
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        //每个分组创建一个ingress
        List<String> protocols;
        if (routeConfig != null) {
            if (!(StringUtils.equals(Net7Protocol.HTTP.getLabel(), routeConfig.getProtocol()))
                    && !(StringUtils.equals(Net7Protocol.HTTPS.getLabel(), routeConfig.getProtocol()))
                    && !(StringUtils.equals(Net7Protocol.HTTP_HTTPS.getLabel(), routeConfig.getProtocol()))) {
                //该端口为TCP和UDP,跳过
                return;
            }
            //判断是要创建黑白名单的plugin
            String kongPluginName = null;
            //创建ingress
            createIngress(routeConfig, kongPluginName, client, deleteFlag, nameSpace);
        }
    }


    /**
     * 创建ingress
     *
     * @param routeConfig 二级路径列表：后端项目就是根路径、前端项目会拼接特殊的路径规则。但最前面一段的路径是一样的。即rootPath
     */
    private void createIngress(K8sRouteConfigVo routeConfig, String kongPluginName, DefaultKubernetesClient client, Boolean deleteFlag, String nameSpace) {
//        String serviceName = String.format("%s-%s", routeConfig.getServiceName(), routeConfig.getPort());
        String serviceName = routeConfig.getServiceName();
        IngressBackend ingressBackend = new IngressBackendBuilder().withServiceName(serviceName).withServicePort(new IntOrStringBuilder().withIntVal(routeConfig.getPort()).build()).build();

        HTTPIngressPath paths = new HTTPIngressPath();
        paths.setBackend(ingressBackend);
        IngressRuleBuilder ingressRuleBuilder = new IngressRuleBuilder();
        if (StringUtils.isNotBlank(routeConfig.getHostList())) {
            ingressRuleBuilder.withHost(routeConfig.getHostList());
        }
        if (StringUtils.isNotBlank(routeConfig.getPath())) {
            paths.setPath(routeConfig.getPath());
        }
        IngressRule ingressRule = new IngressRuleBuilder().editOrNewHttp().addToPaths(paths).endHttp().build();
        IngressBuilder ingressBuilder = new IngressBuilder().editOrNewSpec().withRules(ingressRule).endSpec();

        Map<String, String> annotations = getIngressAnnotations(routeConfig.getProtocol(), serviceName, kongPluginName);
        if (routeConfig.getStripPath() == 1) {
            annotations.put(REWRITE_TARGET, "/");
        }
        ingressBuilder = ingressBuilder.editOrNewMetadata().withAnnotations(annotations).endMetadata();
        log.info("PackageDeployer.createIngress================start 01");

        String ingressName = String.format("%s-%s", routeConfig.getServiceName(), routeConfig.getName());
        log.info("PackageDeployer.createIngress================start 02  ingressName:{}", ingressName);
        Map<String, String> labels = Maps.newHashMap();
        labels.put(PROJECT_MARK, routeConfig.getServiceName());
        labels.put(K8sLabels.CATEGORY, "project");
        labels.put("name", routeConfig.getServiceName());
        ingressBuilder = ingressBuilder.editOrNewMetadata().withName(ingressName).withLabels(labels).endMetadata();

        log.info("PackageDeployer.createIngress================start 03  ");
        Ingress ingress = ingressBuilder.build();
        log.info("PackageDeployer.createIngress================start 04  ");


        log.info("PackageDeployer.createIngress================start 05  ");
        if (deleteFlag) {
            deleteIngress(ingressName, nameSpace);
        } else {
            new K8sIngressOperator(this.k8sClientHolder).createOrReplace(nameSpace, ingress);
//            client.extensions().ingresses().inNamespace(nameSpace).createOrReplace(ingress);
        }
        log.info("PackageDeployer.createIngress================start 06 ");
    }

    /**
     * 生成固定的ingress的annotations
     *
     * @return
     */
    private Map<String, String> getIngressAnnotations(String protocol, String services, String kongPluginName) {
        Map<String, String> result = new HashMap<>(10);
        result.put(SSL_REDIRECT, "false");
        result.put(AFFINITY, "cookie");
        result.put(SESSION_COOKIE_NAME, "route");
        result.put(SESSION_COOKIE_HASH, "sha1");
        result.put(SESSION_COOKIE_PATH, "/");
        result.put(WEB_SOCKET, services);
        result.put(USE_REGEX, "true");

        if (protocol.toUpperCase().equals(Net7Protocol.HTTP.getLabel())) {
            result.put(NGINX_CONFIG_SNIPPET, ingressConfig.getSinppetHttp());
        } else if (protocol.toUpperCase().equals(Net7Protocol.HTTPS.getLabel())) {
            String currentIp = k8sClientHolder.getK8sConfig().getMasters().get(0);
            result.put(NGINX_CONFIG_SNIPPET, String.format(ingressConfig.getSinppetHttps(), StringUtils.equals(currentIp, "192.177.1.100") ? "$host" : currentIp));
        }
        log.info("项目配置了snippet:{}", result.get(NGINX_CONFIG_SNIPPET));

        if (StringUtils.isNotBlank(kongPluginName)) {
            result.put(CONFIGURATION_KONG_PLUGIN, kongPluginName);
        }

        return result;
    }

    /**
     * 创建service
     *
     * @param vo
     */
    public Service createService(K8sServiceDeployVo vo) {
        log.info("update => K8sServiceDeployVo:{}", JSON.toJSONString(vo));
        ServiceBuilder serviceBuilder = new ServiceBuilder();
        // apiVersion
        serviceBuilder.withApiVersion(vo.getApiVersion());

        // metadata.name
        serviceBuilder.withNewMetadata().withName(vo.getName()).endMetadata();

        // metadata.spec
        ServiceSpec spec = new ServiceSpec();

        // metadata.spec.type
        if (StringUtils.isNotBlank(vo.getType())) {
            spec.setType(vo.getType());
        }

        // metadata.spec.ports
        spec.setPorts(vo.getPorts());
        // metadata.spec.selector
        spec.setSelector(vo.getSelector());
        serviceBuilder.withSpec(spec);

        Service service = serviceBuilder.build();
        new K8sServiceOperator(this.k8sClientHolder).createOrReplace(service);
        return service;
    }

    /**
     * 创建deployment
     *
     * @param vo
     */
    public Deployment createDeployment(K8sBuildVo vo) {
        log.info("update => K8sBuildVo:{}", JSON.toJSONString(vo));
        DeploymentBuilder deploymentBuilder = new DeploymentBuilder();
        // 1. apiVersion
        deploymentBuilder.withApiVersion(vo.getApiVersion());

        // 2. kind
        deploymentBuilder.withKind(vo.getKind());

        // 3. metadata:
        // 3.1 name: ps0
        deploymentBuilder.withNewMetadata().withName(vo.getName()).endMetadata();

        // 4. spec:
        deploymentBuilder.withSpec(buildDeploymentSpec(vo));

        Deployment build = deploymentBuilder.build();
        new K8sDeploymentOperator(this.k8sClientHolder).createOrReplace(build);
        return build;

    }


    /**
     * 构建 DeploymentSpec
     *
     * @param vo
     * @return
     */
    private DeploymentSpec buildDeploymentSpec(K8sBuildVo vo) {
        DeploymentSpec spec = new DeploymentSpec();

        // 1 replicas
        if (vo.getReplicas() != null) {
            spec.setReplicas(vo.getReplicas());
        }

        // 2 template
        spec.setTemplate(buildPodTemplateSpec(vo));
        return spec;
    }

    /**
     * 构建PodTemplateSpec
     *
     * @param vo
     * @return
     */
    private PodTemplateSpec buildPodTemplateSpec(K8sBuildVo vo) {
        PodTemplateSpec template = new PodTemplateSpec();
        // metadata
        template.setMetadata(buildObjectMeta(vo));

        // spec
        template.setSpec(buildPodSpec(vo));
        return template;
    }

    /**
     * 构建 ObjectMeta
     *
     * @param vo
     * @return
     */
    private ObjectMeta buildObjectMeta(K8sBuildVo vo) {
        ObjectMeta metadata = new ObjectMeta();
        metadata.setLabels(vo.getLabels());
        return metadata;
    }

    /**
     * 构建 PodSpec
     *
     * @param vo
     * @return
     */
    private PodSpec buildPodSpec(K8sBuildVo vo) {
        PodSpec podSpec = new PodSpec();
        // 1 containers
        podSpec.setContainers(buildContainers(vo));

        // 2 volumes
        podSpec.setVolumes(vo.getVolumes());

        // 3 restartPolicy
        podSpec.setRestartPolicy(vo.getRestartPolicy());
        return podSpec;
    }

    /**
     * 构建容器
     *
     * @param vo
     * @return
     */
    private List<Container> buildContainers(K8sBuildVo vo) {
        List<Container> containers = new ArrayList<>();

        // 构建容器
        Container container = new Container();
        // 1 name
        container.setName(vo.getName());
        // 2 image
        container.setImage(vo.getImage());
        // 3 imagePullPolicy
        container.setImagePullPolicy(vo.getImagePullPolicy());
        // 4 command
        container.setCommand(vo.getCommand());
        // 5 args
        if (vo.getArgs() != null && vo.getArgs().size() > 0) {
            container.setArgs(vo.getArgs());
        }
        // 6 resources 分配资源
        container.setResources(buildResoReq(vo.getGpuSize()));
        // 7 volumeMounts
        container.setVolumeMounts(vo.getVolumeMounts());

        containers.add(container);
        return containers;
    }

    /**
     * 构建资源需求
     *
     * @param gpuSize 显卡数量
     * @return
     */
    private ResourceRequirements buildResoReq(int gpuSize) {
        Map<String, Quantity> limits = new HashMap<>();
        limits.put(K8sConstants.NVIDIA_GPU, new Quantity(String.valueOf(gpuSize)));
        return new ResourceRequirementsBuilder().withLimits(limits).withRequests(limits).build();
    }

    /**
     * 删除 ingress
     *
     * @param name
     * @param nameSpace
     */
    public boolean deleteIngress(String name, String nameSpace) {
        log.info("delete => name:{},nameSpace:{}", name, nameSpace);
        Boolean res = true;
        if (k8sClientHolder.getClient().extensions().ingresses().inNamespace(nameSpace).withName(name) != null) {
            res = new K8sIngressOperator(this.k8sClientHolder).delete(name, nameSpace);
//            res = k8sClientHolder.getClient().extensions().ingresses().inNamespace(nameSpace).withName(name).delete();
        }
        return res;
    }

    /**
     * 删除job（暂停job也是删除）
     *
     * @return
     */
    public Boolean deleteJob(String name, String nameSpace) {
        log.info("delete => name:{},nameSpace:{}", name, nameSpace);
        new K8sJobOperator(this.k8sClientHolder).delete(name, nameSpace);
//        ScalableResource<Job> resource = k8sClientHolder.getClient().batch().jobs().inNamespace(nameSpace).withName(name);
//        if (resource.get() != null) {
//            return resource.delete();
//        }
        return true;

    }

    /**
     * 删除 Deployment （暂停 Deployment 也是删除）
     *
     * @return
     */
    public Boolean deleteDeployment(String name, String nameSpace) {
        log.info("delete => name:{},nameSpace:{}", name, nameSpace);
        Boolean res = true;
        if (k8sClientHolder.getClient().apps().deployments().inNamespace(nameSpace).withName(name) != null
                && k8sClientHolder.getClient().apps().deployments().inNamespace(nameSpace).withName(name).get() != null) {
            res = new K8sDeploymentOperator(this.k8sClientHolder).delete(name, nameSpace);
//            res = k8sClientHolder.getClient().apps().deployments().inNamespace(nameSpace).withName(name).delete();
        }
        return res;
    }

    public boolean deleteDeployObject(String name, String namespace) {
        log.info("delete => name:{},nameSpace:{}", name, namespace);
//        final HasMetadata deployObject = getDeployObject(name, namespace);
//        if (deployObject != null) {
//            k8sClientHolder.getClient().resource(deployObject).inNamespace(namespace).delete();
//            return true;
//        }

        Deployment deployments = this.getDeployment(name, namespace);
        if (deployments != null) {
            new K8sDeploymentOperator(this.k8sClientHolder).delete(name, namespace);
            return true;
        }

        StatefulSet statefulSets = this.getStatefulSet(name, namespace);
        if (statefulSets != null) {
            new K8sStatefulSetOperator(this.k8sClientHolder).delete(name, namespace);
            return true;
        }

        DaemonSet daemonSets = this.getDaemonSet(name, namespace);
        if (daemonSets != null) {
            new K8sDaemonSetOperator(this.k8sClientHolder).delete(name, namespace);
            return true;
        }

        return false;
    }

    /**
     * 删除 Service
     *
     * @return
     */
    public Boolean deleteService(String name, String nameSpace) {
        log.info("delete => name:{},nameSpace:{}", name, nameSpace);
        new K8sServiceOperator(this.k8sClientHolder).delete(name, nameSpace);
//        Resource<Service> resource = k8sClientHolder.getClient().services().inNamespace(nameSpace).withName(name);
//        if (resource.get() != null) {
//            return resource.delete();
//        }
        return true;
    }

    /**
     * /**
     * 获取deployment状态
     *
     * @return
     */
    public DeploymentStatus getDeploymentStatus(String deploymentName, String nameSpace) {
        log.info("get => deploymentName:{},nameSpace:{}", deploymentName, nameSpace);
        Deployment deployment = k8sClientHolder.getClient().apps().deployments().inNamespace(nameSpace).withName(deploymentName).get();
        if (deployment != null) {
            DeploymentStatus status = deployment.getStatus();
            //        Integer succeeded = status.getSucceeded();
            return status;
        } else {
            return null;
        }
    }

    /**
     * 根据name获取PVC
     *
     * @param name
     * @param namespace
     * @return
     */
    public PersistentVolumeClaim getPvcByName(String name, String namespace) {
        log.info("get => name:{},nameSpace:{}", name, namespace);
        PersistentVolumeClaim pvc = k8sClientHolder.getClient().persistentVolumeClaims().inNamespace(namespace).withName(name).get();
        return pvc;
    }


    public Closeable readPodLog(String podName, String namespace, OutputStream stream) {
        log.info("get => podName:{},nameSpace:{},stream:{}", podName, namespace, stream);
        LogWatch logWatch = null;

//        if(podName.contains("mysql")) {
//            logWatch = k8sClientHolder.getClient().pods()
//                    .inNamespace(namespace)
//                    .withName(podName).inContainer("mysql")
//                    .tailingLines(1)
//                    .watchLog(stream);
//        }else{
        log.info(podName);
        logWatch = k8sClientHolder.getClient().pods()
                .inNamespace(namespace)
                .withName(podName)
                .tailingLines(1)
                .watchLog(stream);
//        }
        return logWatch;

    }

    public List<Pod> getPods(String namespace) {
        log.info("get => namespace:{}", namespace);
        return k8sClientHolder.getClient().pods().inNamespace(namespace).list().getItems();
    }

    public List<Pod> getPods() {
        log.info("get => list all");
        return k8sClientHolder.getClient().pods().inAnyNamespace().list().getItems();
    }

    /**
     * 返回服务名称和 pod的对应关系
     *
     * @param namespace
     * @return
     */
    public Map<String, List<Pod>> getServicePods(String namespace, List<String> names) {
        log.info("get => namespace:{},names:{}", namespace, JSON.toJSONString(names));
        Map<String, List<Pod>> result = new HashMap<>();
        for (String name : names) {
            result.put(name, new ArrayList<>());
        }
        List<Pod> podList = getPods(namespace);
        for (Pod pod : podList) {
            for (String name : names) {
                if (pod.getMetadata().getName().contains(name)) {
                    result.get(name).add(pod);
                }
            }
        }
        return result;
    }

    public List<Pod> getPodListByMatchlabels(String name, String namespace) {
        log.info("get => name:{},nameSpace:{}", name, namespace);
        List<Pod> podList = null;
        Deployment deployment = k8sClientHolder.getClient().apps().deployments().inNamespace(namespace).withName(name).get();
        if (deployment != null) {
            Map<String, String> matchLabels = deployment.getSpec().getSelector().getMatchLabels();
            podList = k8sClientHolder.getClient().pods().inNamespace(namespace).withLabels(matchLabels).list().getItems();
        }
        return podList;
    }

    /**
     * 跟据节点名称获取节点
     *
     * @param podName
     * @param namespace
     * @return
     */
    public Pod getPod(String podName, String namespace) {
        log.info("get => podName:{},nameSpace:{}", podName, namespace);
        return k8sClientHolder.getClient().pods().inNamespace(namespace).withName(podName).get();
    }


    /**
     * 获取实际pod名
     *
     * @param name
     * @param namespace
     * @return
     */
    public String getFullPodName(String name, String namespace) {
        log.info("get => name:{},nameSpace:{}", name, namespace);
        String fullPodName = null;
        //获取pod列表
        List<Pod> podList = k8sClientHolder.getClient().pods().inNamespace(namespace).list().getItems();
        //循环根据job获取pod name
        for (Pod pod : podList) {
            if (pod.getMetadata().getName().contains(name)) {
                fullPodName = pod.getMetadata().getName();
                break;
            }
        }
        return fullPodName;
    }

    /**
     * 根据deploymentYaml
     *
     * @param deployment deployment的yaml文件
     */
    public Boolean createDeployment(Deployment deployment, String nameSpace) {
        log.info("update => deployment:{},nameSpace:{}", JSON.toJSONString(deployment), nameSpace);
        try {
            new K8sDeploymentOperator(this.k8sClientHolder).createOrReplace(nameSpace, deployment);
//            k8sClientHolder.getClient().apps().deployments().inNamespace(nameSpace).createOrReplace(deployment);
        } catch (Exception e) {
            log.error("PackageDeployer.createDeployment ----------------- exception : {}", e.getMessage());
            return false;
        }
        return true;
    }

    private Map<String, String> toMap(String key, String value) {
        HashMap<String, String> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    public PodTemplateSpec getPodTemplete(String name, String namespace) {
        log.info("get => name:{},nameSpace:{}", name, namespace);
        List<Deployment> deployments = this.queryDeploymentsByLabel(toMap("name", name), namespace);
        if (deployments != null && !deployments.isEmpty()) {
            return deployments.get(0).getSpec().getTemplate();
        }

        List<DaemonSet> daemonSets = this.queryDaemonSetByLabel(toMap("name", name), namespace);
        if (daemonSets != null && !daemonSets.isEmpty()) {
            return daemonSets.get(0).getSpec().getTemplate();
        }

        List<StatefulSet> statefulSets = this.queryStatefulSetByLabel(toMap("name", name), namespace);
        if (statefulSets != null && !statefulSets.isEmpty()) {
            return statefulSets.get(0).getSpec().getTemplate();
        }
        return null;
    }

    public HasMetadata getK8sModuleData(String name, String namespace) {
        log.info("get => name:{},nameSpace:{}", name, namespace);
        List<HasMetadata> list = queryK8sModuleDataCustom(name, namespace);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }


    public StatefulSet getStatefulSet(String name, String namespace) {
        log.info("get => name:{},nameSpace:{}", name, namespace);
        return k8sClientHolder.getClient().apps().statefulSets().inNamespace(namespace).withName(name).get();
    }


    public DaemonSet getDaemonSet(String name, String namespace) {
        log.info("get => name:{},nameSpace:{}", name, namespace);
        return k8sClientHolder.getClient().apps().daemonSets().inNamespace(namespace).withName(name).get();
    }


    public List<HasMetadata> queryK8sModuleData(Map<String, String> labels, String namespace) {
        log.info("get => labels:{},nameSpace:{}", JSON.toJSONString(labels), namespace);
        List<HasMetadata> list = new ArrayList<>();
        List<Deployment> deployments = this.queryDeploymentsByLabel(labels, namespace);
        if (deployments != null && !deployments.isEmpty()) {
            list.addAll(deployments);
        }

        List<DaemonSet> daemonSets = this.queryDaemonSetByLabel(labels, namespace);
        if (daemonSets != null && !daemonSets.isEmpty()) {
            list.addAll(daemonSets);
        }

        List<StatefulSet> statefulSets = this.queryStatefulSetByLabel(labels, namespace);
        if (statefulSets != null && !statefulSets.isEmpty()) {
            list.addAll(statefulSets);
        }
        return list;
    }


    public List<HasMetadata> queryK8sModuleDataCustom(String name, String namespace) {
        log.info("get => name:{},nameSpace:{}", name, namespace);
        List<HasMetadata> list = new ArrayList<>();
        Deployment deployment = this.getDeployment(name, namespace);
        if (deployment != null) {
            list.add(deployment);
        }

        DaemonSet daemonSet = this.getDaemonSet(name, namespace);
        if (daemonSet != null) {
            list.add(daemonSet);
        }

        StatefulSet statefulSet = this.getStatefulSet(name, namespace);
        if (statefulSet != null) {
            list.add(statefulSet);
        }
        return list;
    }

    /**
     * 查询部署的服务data信息，
     *
     * @param name      服务名
     * @param namespace
     * @return
     */
    public List<HasMetadata> queryK8sModuleData(String name, String namespace) {
        log.info("get => name:{},nameSpace:{}", name, namespace);
        return queryK8sModuleData(toMap("app.kubernetes.io/instance", name), namespace);
    }

    public List<HasMetadata> queryK8sModuleData(String labelKey, String labelValue, String namespace) {
        log.info("get => labelKey:{},labelValue:{},namespace:{}", labelKey, labelValue, namespace);
        return queryK8sModuleData(toMap(labelKey, labelValue), namespace);
    }

    public List<Pod> queryK8sPods(String deployName, String namespace) {
        log.info("get => deployName:{},nameSpace:{}", deployName, namespace);
        LabelSelector labelSelector = getDeploymentLabelSelector(deployName, namespace);
        if (labelSelector == null) {
            return new ArrayList<>();
        }
        return k8sClientHolder.getClient().pods().inNamespace(namespace).withLabelSelector(labelSelector).list().getItems();
    }

    public List<Volume> getDeployObjectVolumes(String deployName, String namespace) {
        log.info("get => deployName:{},nameSpace:{}", deployName, namespace);
        Deployment deployments = this.getDeployment(deployName, namespace);
        if (deployments != null) {
            return deployments.getSpec().getTemplate().getSpec().getVolumes();
        }

        StatefulSet statefulSets = this.getStatefulSet(deployName, namespace);
        if (statefulSets != null) {
            return statefulSets.getSpec().getTemplate().getSpec().getVolumes();
        }

        DaemonSet daemonSets = this.getDaemonSet(deployName, namespace);
        if (daemonSets != null) {
            return daemonSets.getSpec().getTemplate().getSpec().getVolumes();
        }
        return null;
    }

    public ObjectMeta getDeployPodMeta(String deployName, String namespace) {
        log.info("get => deployName:{},nameSpace:{}", deployName, namespace);
        Deployment deployments = this.getDeployment(deployName, namespace);
        if (deployments != null) {
            return deployments.getSpec().getTemplate().getMetadata();
        }

        StatefulSet statefulSets = this.getStatefulSet(deployName, namespace);
        if (statefulSets != null) {
            return statefulSets.getSpec().getTemplate().getMetadata();
        }

        DaemonSet daemonSets = this.getDaemonSet(deployName, namespace);
        if (daemonSets != null) {
            return daemonSets.getSpec().getTemplate().getMetadata();
        }
        return null;
    }


    public HasMetadata getDeployObject(String deployName, String namespace) {
        log.info("get => deployName:{},nameSpace:{}", deployName, namespace);
        Deployment deployments = this.getDeployment(deployName, namespace);
        if (deployments != null) {
            return deployments;
        }

        StatefulSet statefulSets = this.getStatefulSet(deployName, namespace);
        if (statefulSets != null) {
            return statefulSets;
        }

        DaemonSet daemonSets = this.getDaemonSet(deployName, namespace);
        if (daemonSets != null) {
            return daemonSets;
        }
        return null;
    }


    private LabelSelector getDeploymentLabelSelector(String deployName, String namespace) {
        log.info("get => deployName:{},nameSpace:{}", deployName, namespace);
        Deployment deployments = this.getDeployment(deployName, namespace);
        if (deployments != null) {
            return deployments.getSpec().getSelector();
        }

        DaemonSet daemonSets = this.getDaemonSet(deployName, namespace);
        if (daemonSets != null) {
            return daemonSets.getSpec().getSelector();
        }

        StatefulSet statefulSets = this.getStatefulSet(deployName, namespace);
        if (statefulSets != null) {
            return statefulSets.getSpec().getSelector();
        }
        return null;
    }


    /**
     * 查询deployment
     *
     * @param name
     */
    public Deployment getDeployment(String name, String nameSpace) {
        log.info("get => name:{},nameSpace:{}", name, nameSpace);
        Deployment result = null;
        try {
            result = k8sClientHolder.getClient().apps().deployments().inNamespace(nameSpace).withName(name).get();
        } catch (Exception e) {
            log.error("PackageDeployer.getDeployment ----------------- exception : {}", e.getMessage());
        }
        return result;
    }

    public List<Deployment> queryDeploymentsByLabel(Map<String, String> labels, String namespace) {
        log.info("get => labels:{},nameSpace:{}", JSON.toJSONString(labels), namespace);
        DeploymentList deploymentList =
                k8sClientHolder.getClient().apps()
                        .deployments().inNamespace(namespace)
                        .withLabels(labels).list();
        if (deploymentList == null || CollectionUtils.isEmpty(deploymentList.getItems())) {
            return null;
        }
        return deploymentList.getItems();
    }


    public List<DaemonSet> queryDaemonSetByLabel(String name, String value) {
        log.info("get => name:{},nameSpace:{}", name, value);
        DaemonSetList daemonSetList =
                k8sClientHolder.getClient().apps()
                        .daemonSets().inAnyNamespace()
                        .withLabel(name, value).list();
        if (daemonSetList == null || CollectionUtils.isEmpty(daemonSetList.getItems())) {
            return null;
        }
        return daemonSetList.getItems();
    }

    public List<DaemonSet> queryDaemonSetByLabel(Map<String, String> labels, String namespace) {
        log.info("get => labels:{},nameSpace:{}", JSON.toJSONString(labels), namespace);
        DaemonSetList daemonSetList =
                k8sClientHolder.getClient().apps()
                        .daemonSets().inNamespace(namespace)
                        .withLabels(labels).list();
        if (daemonSetList == null || CollectionUtils.isEmpty(daemonSetList.getItems())) {
            return null;
        }
        return daemonSetList.getItems();
    }

    public List<StatefulSet> queryStatefulSetByLabel(Map<String, String> labels, String namespace) {
        log.info("get => labels:{},nameSpace:{}", JSON.toJSONString(labels), namespace);
        StatefulSetList statefulSetList =
                k8sClientHolder.getClient().apps()
                        .statefulSets().inNamespace(namespace)
                        .withLabels(labels).list();
        if (statefulSetList == null || CollectionUtils.isEmpty(statefulSetList.getItems())) {
            return null;
        }
        return statefulSetList.getItems();
    }


    public List<Pod> queryPods(Map<String, String> labels, String namespace) {
        log.info("get => labels:{},nameSpace:{}", JSON.toJSONString(labels), namespace);
        return k8sClientHolder.getClient().pods().inNamespace(namespace).withLabels(labels).list().getItems();
    }


    public List<Service> queryServiceByDeployment(String deploymentName, String namespace) {
        log.info("get => deploymentName:{},nameSpace:{}", deploymentName, namespace);
        List<Service> result = new ArrayList<>();
        List<Pod> list = k8sClientHolder.getClient().pods().inNamespace(namespace).withLabel("name", deploymentName).list().getItems();
        if (list == null || list.isEmpty()) {
            return result;
        }
        List<Endpoints> endpointList = k8sClientHolder.getClient().endpoints().inNamespace(namespace).list().getItems();
        if (endpointList == null || endpointList.isEmpty()) {
            return result;
        }

        List<String> podNames = list.stream().map(pod -> pod.getMetadata().getName()).collect(Collectors.toList());
        for (Endpoints endpoints : endpointList) {
            Service service = getService(podNames, endpoints);
            if (service != null) {
                result.add(service);
            }
        }
        return result;
    }

    private Service getService(List<String> podNames, Endpoints endpoints) {
        log.info("get => podNames:{},endpoints:{}", JSON.toJSONString(podNames), JSON.toJSONString(endpoints));
        for (EndpointSubset subset : endpoints.getSubsets()) {
            for (EndpointAddress address : subset.getAddresses()) {
                if (address.getTargetRef() == null) {
                    continue;
                }
                if (podNames.contains(address.getTargetRef().getName())) {
                    return getService(endpoints.getMetadata().getName(), endpoints.getMetadata().getNamespace());
                }
            }
        }
        return null;
    }


    public List<HasMetadata> getK8sModuleByService(String serviceName, String namespace) {
        log.info("get => serviceName:{},String:{}", serviceName, namespace);
        Service service = getService(serviceName, namespace);
        if (service == null) {
            return Lists.newArrayList();
        }
        List<Pod> pods = queryPods(service.getSpec().getSelector(), namespace);
        if (pods == null || pods.isEmpty()) {
            return Lists.newArrayList();
        }

        Set<String> deployments = pods.stream().map(pod -> pod.getMetadata().getLabels().get("name")).collect(Collectors.toSet());
        return deployments.stream().filter(deploy -> StringUtils.isNotBlank(deploy))
                .map(deploy -> getK8sModuleData(deploy, namespace)).collect(Collectors.toList());

    }

    public List<Service> queryService(String namespace) {
        log.info("get => namespace:{}", namespace);
        return k8sClientHolder.getClient().services().inNamespace(namespace).list().getItems();
    }

    public Service getService(String name, String namespace) {
        log.info("get => name:{},namespace:{}", name, namespace);
        return k8sClientHolder.getClient().services().inNamespace(namespace).withName(name).get();
    }

    public List<Service> queryServiceWithLabel(Map<String, String> labels, String namespace) {
        log.info("get => labels:{},namespace:{}", labels, namespace);
        return k8sClientHolder.getClient().services().inNamespace(namespace).withLabels(labels).list().getItems();
    }

    public void deleteService(List<String> names, String namespace) {
        log.info("get => names:{},namespace:{}", JSON.toJSONString(names), namespace);
        for (String name : names) {
            new K8sServiceOperator(this.k8sClientHolder).delete(name, namespace);
//            k8sClientHolder.getClient().services().inNamespace(namespace).withName(name).delete();
        }
    }

    public void createService(String namespace, Service service) {
        log.info("update => namespace:{},service:{}", namespace, JSON.toJSONString(service));
//        k8sClientHolder.getClient().services().inNamespace(namespace).createOrReplace(service);
        new K8sServiceOperator(this.k8sClientHolder).createOrReplace(namespace, service);
    }

    public Node getNode(String k8sNodeName) {
        log.info("get => k8sNodeName:{}", k8sNodeName);
        return k8sClientHolder.getClient().nodes().withName(k8sNodeName).get();
    }

    public boolean deletePod(Pod pod, String namespace) {
        log.info("get => pod:{}", JSON.toJSONString(pod));
        boolean result = new K8sPodOperator(this.k8sClientHolder).delete(pod.getMetadata().getName(), pod.getMetadata().getNamespace());
        return result;
    }

    public boolean patchSchedulingStatus(String k8sNodeName, boolean status) {
        log.info("update => k8sNodeName:{},status:{}", k8sNodeName, status);
        Node node = this.getNode(k8sNodeName);
        if (node == null) {
            throw new NodeNullPointerException("集群中未查到主机！");
        }
        node.getSpec().setUnschedulable(!status);
        new K8sNodeOperator(this.k8sClientHolder).createOrReplace(node);
//        k8sClientHolder.getClient().nodes().createOrReplace(node);
        return true;
    }

    public List<StorageClass> getStorageClasses() {
        return k8sClientHolder.getClient().storage().storageClasses().list().getItems();
    }

    public boolean podComplete(String podPrefixName, String... namespaces) {
        try {
            boolean flag = false;
            //5分钟 harbor还没起来，就直接报错
            for (int i = 0; i < 100; i++) {
                try {
                    Thread.sleep(3000);
                    List<Pod> glusterfsList = Lists.newArrayList();
                    List<Pod> podList = Lists.newArrayList();
                    for (String namespace : namespaces) {
                        podList = k8sClientHolder.getClient().pods().inNamespace(namespace).list().getItems();
                        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(podList)) {
                            break;
                        }
                    }
                    for (Pod pod : podList) {
                        if (pod.getMetadata().getName().startsWith(podPrefixName)) {
                            log.info("{} podName:{}", podPrefixName, pod.getMetadata().getName());
                            glusterfsList.add(pod);
                        }
                    }
                    if (CollectionUtils.isEmpty(glusterfsList)) {
                        continue;
                    }
                    int count = glusterfsList.size();
                    for (Pod pod : glusterfsList) {
                        boolean b = false;
                        for (ContainerStatus status : pod.getStatus().getContainerStatuses()) {
                            if (status.getReady()) {
                                b = true;
                                break;
                            }
                        }
                        if (b) {
                            count--;
                        }
                    }
                    if (count == 0) {
                        flag = true;
                        break;
                    }
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                }
            }
            return flag;
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        return false;
    }

    public boolean podComplete(String namespace, String podPrefixName) {
        log.info("update => namespace:{},podPrefixName:{}", namespace, podPrefixName);
        try {
            boolean flag = false;
            //5分钟 harbor还没起来，就直接报错
            for (int i = 0; i < 100; i++) {
                try {
                    Thread.sleep(3000);
                    List<Pod> glusterfsList = Lists.newArrayList();
                    List<Pod> podList = k8sClientHolder.getClient().pods().inNamespace(namespace).list().getItems();
                    for (Pod pod : podList) {
                        if (pod.getMetadata().getName().startsWith(podPrefixName)) {
                            log.info("{} podName:{}", podPrefixName, pod.getMetadata().getName());
                            glusterfsList.add(pod);
                        }
                    }
                    if (CollectionUtils.isEmpty(glusterfsList)) {
                        continue;
                    }
                    int count = glusterfsList.size();
                    for (Pod pod : glusterfsList) {
                        boolean b = false;
                        for (ContainerStatus status : pod.getStatus().getContainerStatuses()) {
                            if (status.getReady()) {
                                b = true;
                                break;
                            }
                        }
                        if (b) {
                            count--;
                        }
                    }
                    if (count == 0) {
                        flag = true;
                        break;
                    }
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                }
            }
            return flag;
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        return false;
    }


    public boolean podComplete(String namespace, Map<String, String> matchLabels) {
        try {
            boolean flag = false;
            //5分钟 还没起来，就直接报错
            for (int i = 0; i < 100; i++) {
                try {
                    Thread.sleep(3000);
                    List<Pod> podList = k8sClientHolder.getClient().pods().inNamespace(namespace).withLabels(matchLabels).list().getItems();
                    if (CollectionUtils.isEmpty(podList)) {
                        continue;
                    }
                    int count = podList.size();
                    for (Pod pod : podList) {
                        boolean b = false;
                        for (ContainerStatus status : pod.getStatus().getContainerStatuses()) {
                            if (status.getReady()) {
                                b = true;
                                break;
                            }
                        }
                        if (b) {
                            count--;
                        }
                    }
                    if (count == 0) {
                        flag = true;
                        break;
                    }
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                }
            }
            return flag;
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        return false;
    }


    private List<Pod> queryK8sPodsUntilExists(String deployName, String namespace, Callback back, int sum) {
        List<Pod> pods = queryK8sPods(deployName, namespace);
        if (org.apache.commons.collections.CollectionUtils.isEmpty(pods)) {
            if (sum < 0) {
                throw new RuntimeException("未找到指定服务名的pods, deployName=" + deployName + ",namespace=" + namespace);
            }
            try {
                back.accept("wait for pods [" + deployName + "] ready, 10s try again...");
                TimeUnit.SECONDS.sleep(10);
                return queryK8sPodsUntilExists(deployName, namespace, back, sum - 1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return pods;
    }

    public void execCommand(String deployName, String namespace, List<String> commands, Callback back) {
        Pod readyPod = getReadyPod(deployName, namespace, back);
        execCommand(readyPod, namespace, commands, back);
    }

    public void execCommand(Pod readyPod, String namespace, List<String> commands, Callback back) {
        back.accept("exec in pod, podsName=" + readyPod.getMetadata().getName() + ",namespace=" + namespace);
        execShellInPod(readyPod.getMetadata().getName(), namespace, commands, back);
    }

    public void execCommand(String deployName, String namespace, List<String> commands, Callback back, long time) {
        Pod readyPod = getReadyPod(deployName, namespace, back);
        execCommand(readyPod, namespace, commands, back, time);
    }

    public void execCommand(Pod readyPod, String namespace, List<String> commands, Callback back, long time) {
        back.accept("exec in pod, podsName=" + readyPod.getMetadata().getName() + ",namespace=" + namespace + ",time" +
                "=" + time);
        execCustom(readyPod.getMetadata().getName(), namespace, commands, back, 3, time);
    }

    public Pod getReadyPod(String deployName, String namespace, Callback back) {
        List<Pod> pods = queryK8sPodsUntilExists(deployName, namespace, back, 60);
        Pod readyPod = waitReadyPod(namespace, back, pods);
        if (readyPod == null) {
            throw new RuntimeException("pod not ready, deployName=" + deployName + ",namespace=" + namespace);
        }
        return readyPod;
    }

    public List<Pod> getReadyPods(String deployName, String namespace, Integer timeout, Callback back) {
        List<Pod> pods = null;
        try {
            pods = queryK8sPodsUntilExists(deployName, namespace, back, timeout / 10);
        } catch (RuntimeException e) {
            back.accept(e.getMessage());
            return pods;
        }
        List<Pod> collect = pods.stream().map(pod -> {
            String status = "ready";
            try {
                k8sClientHolder.getClient().resource(pod).inNamespace(namespace).waitUntilReady(timeout, TimeUnit.SECONDS);
                return pod;
            } catch (Exception e) {
                status = "unready";
                log.warn(e.getMessage());
            } finally {
                String message = String.format("pod wait until ready, max %d seconds, podName=%s, namespace=%s, status=%s",
                        timeout, pod.getMetadata().getName(), namespace, status);
                back.accept(message);
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        back.accept(String.format("pods ready %d/%d", collect.size(), pods.size()));
        return collect;
    }

    private Pod waitReadyPod(String namespace, Callback back, List<Pod> pods) {
        Exception throwe = null;
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < MAX_WAIT_POD_TIMEOUT_MS) {
            for (Pod pod : pods) {
                try {
                    back.accept("pod wait until ready, max 10 minutes, podName=" + pod.getMetadata().getName() + ",namespace=" + namespace);
                    k8sClientHolder.getClient().resource(pod).inNamespace(namespace).waitUntilReady(10, TimeUnit.SECONDS);
                    return pod;
                } catch (Exception e) {
                    throwe = e;
                    log.error("pod not ready， ", e);
                }
            }
        }

        if (throwe != null) {
            throw new RuntimeException("pod not ready, namespace=" + namespace);
        }
        return null;

    }

    /**
     * 带重试次数的方案
     *
     * @param name
     * @param namespace
     * @param commands
     * @param back
     * @param sum
     * @param time
     * @return
     */
    private boolean exec(String name, String namespace, List<String> commands, Callback back, int sum, long time) {
        Pod pod = k8sClientHolder.getClient().pods().inNamespace(namespace).withName(name).get();
        String containerName = pod.getSpec().getContainers().get(0).getName();
        back.accept(" connect container name=" + containerName);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
             ExecWatch exec = k8sClientHolder.getClient().pods().inNamespace(namespace).withName(name).inContainer(containerName)
                     .redirectingInput().writingOutput(out).exec("sh")) {

            back.accept(" check pipe is connect...");
            exec.getInput().write("ls \n".getBytes());
            TimeUnit.SECONDS.sleep(2);
            out.reset();
            back.accept(" pods exec pipe is connect, start exec command...");

            for (String cmd : commands) {
                back.accept("exec commands=" + cmd);
                exec.getInput().write((cmd + " \n").getBytes());
                TimeUnit.SECONDS.sleep(1);
                back.accept(out.toString());
                out.reset();
            }

            String endFlag = RandomStringUtils.random(10);
            String endCmd = "echo " + endFlag;
            int count = 0;
            for (; ; ) {
                exec.getInput().write((endCmd + " \n").getBytes());
                TimeUnit.SECONDS.sleep(1);
                String result = out.toString();
                if (result.contains(endFlag)) {
                    log.info("exec cmd end flag: {}", endFlag);
                    break;
                }
                if (StringUtils.isNotBlank(result)) {
                    back.accept(result);
                }
                out.reset();
                if (count > time) {
                    break;
                }
                count++;
            }

            exec.getInput().write("exit \n".getBytes());
            try {
                exec.getInput().close();
            } catch (Exception e) {
            }
            ;

            return true;
        } catch (Exception e) {
            if (sum > 0) {
                back.accept(" pods exec pipe not connect, try again in 10 seconds... ");
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException ie) {
                    log.warn("TimeUnit.SECONDS.sleep(10)", ie);
                }
                exec(name, namespace, commands, back, sum - 1, time);
            }
            throw new RuntimeException("执行命令过程中异常");
        }
    }


    /**
     * 带重试次数的方案
     *
     * @param name
     * @param namespace
     * @param commands
     * @param back
     * @param sum
     * @param time
     * @return
     */
    private boolean execCustom(String name, String namespace, List<String> commands, Callback back, int sum, long time) {
        Pod pod = k8sClientHolder.getClient().pods().inNamespace(namespace).withName(name).get();
        String containerName = pod.getSpec().getContainers().get(0).getName();
        back.accept(" connect container name=" + containerName);
        try (
                ExecWatch exec = k8sClientHolder.getClient().pods().inNamespace(namespace).withName(name).inContainer(containerName)
                        .redirectingInput()
                        .redirectingOutput()
                        .redirectingError()
                        .redirectingErrorChannel()
                        .withTTY()
                        .exec()) {

            String endString = "Command execution End";

            back.accept(" check pipe is connect...");
            exec.getInput().write("ls \n".getBytes());
            back.accept(" pods exec pipe is connect, start exec command...");

            String result = execCmds(exec, commands, endString);

            result = dealResult(result, endString);

            back.accept(result);

            exec.close();
            return true;

        } catch (Exception e) {
            if (sum > 0) {
                back.accept(" pods exec pipe not connect, try again in 10 seconds... ");
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException ie) {
                    log.warn("TimeUnit.SECONDS.sleep(10)", ie);
                }
                execCustom(name, namespace, commands, back, sum - 1, time);
            }
            throw new RuntimeException("执行命令过程中异常");
        }
    }

    private String dealResult(String result, String endString) {
        result = StringUtils.replaceAll(result, "\r\r\n", StringUtils.EMPTY);
        int count = StringUtils.countMatches(result, endString);
        if (count == 2) {
            result = result.substring(result.indexOf(endString) + endString.length());
            result = result.substring(0, result.indexOf(endString));
        }
        if (count == 1) {
            result = result.substring(result.indexOf(endString) + endString.length());
        }
        return result;
    }

    /**
     * 不带重试次数的方案
     *
     * @param name
     * @param namespace
     * @param commands
     * @param back
     * @param sum
     * @return
     */
    private boolean exec(String name, String namespace, List<String> commands, Callback back, int sum) {
        Pod pod = k8sClientHolder.getClient().pods().inNamespace(namespace).withName(name).get();
        String containerName = pod.getSpec().getContainers().get(0).getName();
        back.accept(" connect container name=" + containerName);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
             ExecWatch exec = k8sClientHolder.getClient().pods().inNamespace(namespace).withName(name).inContainer(containerName)
                     .redirectingInput().writingOutput(out).exec("sh")) {

            back.accept(" check pipe is connect...");
            exec.getInput().write("ls \n".getBytes());
            TimeUnit.SECONDS.sleep(2);
            out.reset();
            back.accept(" pods exec pipe is connect, start exec command...");

            for (String cmd : commands) {
                back.accept("exec commands=" + cmd);
                exec.getInput().write((cmd + " \n").getBytes());
                TimeUnit.SECONDS.sleep(1);
                back.accept(out.toString());
                out.reset();
            }

            exec.getInput().write("exit \n".getBytes());
            TimeUnit.SECONDS.sleep(2);
            try {
                exec.getInput().close();
            } catch (Exception e) {
            }
            ;

            return true;
        } catch (Exception e) {
            if (sum > 0) {
                back.accept(" pods exec pipe not connect, try again in 10 seconds... ");
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException ie) {
                    log.warn("TimeUnit.SECONDS.sleep(10)", ie);
                }
                exec(name, namespace, commands, back, sum - 1);
            }
            throw new RuntimeException("执行命令过程中异常");
        }
    }


    /**
     * 不带重试次数的方案
     *
     * @param name
     * @param namespace
     * @param commands
     * @param back
     * @param sum
     * @return
     */
    private boolean execCustom(String name, String namespace, List<String> commands, Callback back, int sum) {
        log.info("开始执行自定义方法....");
        Pod pod = k8sClientHolder.getClient().pods().inNamespace(namespace).withName(name).get();
        String containerName = pod.getSpec().getContainers().get(0).getName();
        back.accept(" connect container name=" + containerName);
        try (
                ExecWatch exec = k8sClientHolder.getClient().pods().inNamespace(namespace).withName(name).inContainer(containerName)
                        .redirectingInput()
                        .redirectingOutput()
                        .redirectingError()
                        .redirectingErrorChannel()
                        .withTTY()
                        .exec()) {

            back.accept(" check pipe is connect...");
            exec.getInput().write("ls \n".getBytes());
            back.accept(" pods exec pipe is connect, start exec command...");

            String endString = "Command execution End";

            String result = execCmds(exec, commands, endString);
            result = dealResult(result, endString);

            back.accept(result);
            exec.close();
            return true;
        } catch (Exception e) {
            if (sum > 0) {
                back.accept(" pods exec pipe not connect, try again in 10 seconds... ");
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException ie) {
                    log.warn("TimeUnit.SECONDS.sleep(10)", ie);
                }
                execCustom(name, namespace, commands, back, sum - 1);
            }
            throw new RuntimeException("执行命令过程中异常");
        }
    }


    public static CompletableFuture<Boolean> pump(InputStream in, InputStreamPumper.Writable out, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                InputStreamPumper.transferTo(in, out);
            } catch (Exception e) {
                log.info(e.getMessage());
            }
            return true;
        }, executor);
    }


    private String execCmds(ExecWatch watch, List<String> cmds, String endString) {

        ByteArrayOutputStream result = new ByteArrayOutputStream(1024);

        CompletableFuture<Boolean> pump = pump(watch.getOutput(), (b, o, l) -> {
                    // 这里获取的字符串 并不是执行一行命令输出一行。而是执行多行后，一起输出的字符串
                    result.write(b, o, l);
                    int count = StringUtils.countMatches(result.toString(), endString);
                    if (count == 2) {
                        // 结束
                        throw new IOException();
                    }
                },
                executorService);

        try {


            for (int i = 0; i < cmds.size(); i++) {
                watch.getInput().write((cmds.get(i) + "\n").getBytes());
                watch.getInput().flush();
            }


            watch.getInput().write(("exit;\n").getBytes());
            watch.getInput().flush();

            watch.getInput().write(("echo " + endString + "\n").getBytes());
            watch.getInput().flush();

            pump.get(60, TimeUnit.SECONDS);

        } catch (java.io.IOException e) {
            log.warn("server error:", e);
        } catch (java.lang.InterruptedException e) {
            log.warn("server error:", e);
        } catch (TimeoutException e) {
            log.warn("timeout:", e);
        } catch (java.util.concurrent.ExecutionException e) {
            log.warn("concurrent error:", e);
        }

        return result.toString();
    }


    public boolean execShellInPod(String name, String namespace, List<String> commands, Callback back) {
        return execCustom(name, namespace, commands, back, 3);
    }

    public void deleteAllVolumePvPvc(String deployName, String namespace) {
        log.info("delete => deployName:{},namespace:{}", deployName, namespace);

        final List<Volume> deployObjectVolumes = getDeployObjectVolumes(deployName, namespace);
        if (deployObjectVolumes != null) {
            deleteAllVolumePvPvc(namespace, getDeployObjectVolumes(deployName, namespace));
        }
    }


    public void deleteAllVolumePvPvc(String namespace, List<Volume> volumes) {
        log.info("delete => namespace:{},volumes:{}", namespace, JSON.toJSONString(volumes));
        List<String> pvcNames = Lists.newArrayList();
        if (org.apache.commons.collections.CollectionUtils.isEmpty(volumes)) {
            return;
        }
        for (Volume volume : volumes) {
            if (volume.getPersistentVolumeClaim() != null
                    && !StringUtils.contains(volume.getPersistentVolumeClaim().getClaimName(), "-share")) {
                pvcNames.add(volume.getPersistentVolumeClaim().getClaimName());
            }
        }
        if (org.apache.commons.collections.CollectionUtils.isEmpty(pvcNames)) {
            return;
        }
        try {
            for (String pvcName : pvcNames) {
                PersistentVolumeClaim pvc = k8sClientHolder.getClient().persistentVolumeClaims().inNamespace(namespace).withName(pvcName).get();
                String pvName = pvc.getSpec().getVolumeName();
                PersistentVolume pv = null;
                if (StringUtils.isNotEmpty(pvName)) {
                    pv = k8sClientHolder.getClient().persistentVolumes().withName(pvName).get();
                }
                //删除pvc
                new K8sPvcOperator(this.k8sClientHolder).delete(pvc);
//                k8sClientHolder.getClient().persistentVolumeClaims().inNamespace(namespace).delete(pvc);
                if (pv == null) {
                    continue;
                }
                //删除pv
                new K8sPvOperator(this.k8sClientHolder).delete(pv);
//                k8sClientHolder.getClient().persistentVolumes().delete(pv);
            }
        } catch (Exception e) {
            log.error("delete volume pv pvc error", e);
        }
    }


    public void deleteAllByClientId(String clientId, String namespace) {
        log.info("delete => clientId:{},namespace:{}", clientId, namespace);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        new K8sSecretOperator(this.k8sClientHolder).deleteWithLabelIn(namespace, LABEL_INSTANCE, clientId);

        new K8sIngressOperator(this.k8sClientHolder).deleteWithLabelIn(namespace, LABEL_INSTANCE, clientId);

        new K8sServiceOperator(this.k8sClientHolder).deleteWithLabelIn(namespace, LABEL_INSTANCE, clientId);

        new K8sJobOperator(this.k8sClientHolder).deleteWithLabelIn(namespace, LABEL_INSTANCE, clientId);

        new K8sCornJobOperator(this.k8sClientHolder).deleteWithLabelIn(namespace, LABEL_INSTANCE, clientId);

        //new K8sDeploymentOperator(this.k8sClientHolder).deleteWithLabelIn(namespace, LABEL_INSTANCE, clientId);

        //new K8sStatefulSetOperator(this.k8sClientHolder).deleteWithLabelIn(namespace, LABEL_INSTANCE, clientId);

        //new K8sDaemonSetOperator(this.k8sClientHolder).deleteWithLabelIn(namespace, LABEL_INSTANCE, clientId);

        //new K8sPodOperator(this.k8sClientHolder).deleteWithLabelIn(namespace, LABEL_INSTANCE, clientId);
    }

    public void deleteAllByClientId(String clientId, String namespace, JSONObject params) {
        log.info("delete => clientId:{},namespace:{}", clientId, namespace);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        if (params.containsKey(K8sResourceTypeEnum.Secret.name())) {
            new K8sSecretOperator(this.k8sClientHolder).deleteWithLabelIn(namespace, LABEL_INSTANCE, clientId);
        }

        if (params.containsKey(K8sResourceTypeEnum.Ingress.name())) {
            new K8sIngressOperator(this.k8sClientHolder).deleteWithLabelIn(namespace, LABEL_INSTANCE, clientId);
        }

        if (params.containsKey(K8sResourceTypeEnum.Service.name())) {
            new K8sServiceOperator(this.k8sClientHolder).deleteWithLabelIn(namespace, LABEL_INSTANCE, clientId);
        }

        if (params.containsKey(K8sResourceTypeEnum.Job.name())) {
            new K8sJobOperator(this.k8sClientHolder).deleteWithLabelIn(namespace, LABEL_INSTANCE, clientId);
        }

        if (params.containsKey(K8sResourceTypeEnum.CronJob.name())) {
            new K8sCornJobOperator(this.k8sClientHolder).deleteWithLabelIn(namespace, LABEL_INSTANCE, clientId);
        }


        //new K8sDeploymentOperator(this.k8sClientHolder).deleteWithLabelIn(namespace, LABEL_INSTANCE, clientId);

        //new K8sStatefulSetOperator(this.k8sClientHolder).deleteWithLabelIn(namespace, LABEL_INSTANCE, clientId);

        //new K8sDaemonSetOperator(this.k8sClientHolder).deleteWithLabelIn(namespace, LABEL_INSTANCE, clientId);

        //new K8sPodOperator(this.k8sClientHolder).deleteWithLabelIn(namespace, LABEL_INSTANCE, clientId);
    }

    public void deleteTcpOrUdp(String clientId, String namespace) {
        log.info("delete => clientId:{},namespace:{}", clientId, namespace);
        List<Service> serviceList = k8sClientHolder.getClient().services().inNamespace(namespace).withLabel("app.kubernetes.io/instance", clientId).list().getItems();
        if (org.apache.commons.collections.CollectionUtils.isEmpty(serviceList)) {
            serviceList = k8sClientHolder.getClient().services().inNamespace(namespace).withLabel("dolphin/client_id", clientId).list().getItems();
            if (org.apache.commons.collections.CollectionUtils.isEmpty(serviceList)) {
                return;
            }
        }

//        Map<String, String> labels = Maps.newHashMap();
//        labels.put("app.kubernetes.io/name", "ingress-nginx");
//        labels.put("app.kubernetes.io/part-of", "ingress-nginx");
        K8sConfigmapOperator configmapOperator = new K8sConfigmapOperator(k8sClientHolder);
        List<ConfigMap> tcpOrUdpConfigmaps = configmapOperator.listByLabel(null, "app.kubernetes.io/name", "ingress-nginx", "ingress");
        for (ConfigMap cm : tcpOrUdpConfigmaps) {
            if (cm.getData() == null) {
                continue;
            }
            List<String> delHostPorts = Lists.newArrayList();
            Map<String, String> filter = Maps.newHashMap();
            Iterator<Map.Entry<String, String>> iterator = cm.getData().entrySet().iterator();
            boolean delFlag = false;
            while (iterator.hasNext()) {
                String hostPort = iterator.next().getKey();
                if ("15000".equals(hostPort)) {
                    continue;
                }
                if (filter.get(hostPort) != null) {
                    continue;
                }
                filter.put(hostPort, "flag");
                String serviceUrl = cm.getData().get(hostPort);
                String[] args = serviceUrl.split(":");
                if (args.length != 2) {
                    continue;
                }
                String serviceNamespace = null;
                String serviceName = args[0];
                String containerPort = args[1];
                int _index = serviceName.indexOf("/");
                if (_index > 0) {
                    serviceNamespace = serviceName.substring(0, _index);
                    serviceName = serviceName.substring(_index + 1);
                }

                int _lastIndex = serviceName.indexOf(containerPort);
                if (_lastIndex > 0) {
                    serviceName = serviceName.substring(0, _lastIndex - 1);
                }
                for (Service svc : serviceList) {
                    if (StringUtils.equals(svc.getMetadata().getName(), serviceName)
                            && StringUtils.equals(svc.getMetadata().getNamespace(), serviceNamespace)) {
                        delHostPorts.add(hostPort);
                        log.info(String.format("remove [%s] from %s/%s", hostPort, cm.getMetadata().getNamespace(), cm.getMetadata().getName()));
                        iterator.remove();
                        delFlag = true;
                    }
                }
                if (delFlag) {
                    configmapOperator.updateTcpOrUdpCm(cm.getMetadata().getName(), cm.getMetadata().getNamespace(), cm);
                    delFromIngressController(cm.getMetadata().getName(), cm.getMetadata().getNamespace(), delHostPorts);
                }
            }
        }

    }

    private void delFromIngressController(String cmName, String cmNamespace, List<String> delHostPorts) {
        K8sDaemonSetOperator k8sDaemonSetOperator = new K8sDaemonSetOperator(k8sClientHolder);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        List<DaemonSet> ingressControllers = k8sDaemonSetOperator.listByLabel(cmNamespace, "app.kubernetes.io/name", "ingress");
        if (org.apache.commons.collections.CollectionUtils.isEmpty(ingressControllers)) {
            ingressControllers = k8sDaemonSetOperator.listByLabel(cmNamespace, "app.type", "ingress-controller");
            if (org.apache.commons.collections.CollectionUtils.isEmpty(ingressControllers)) {
                ingressControllers = k8sDaemonSetOperator.listByLabel(cmNamespace, "iomp.component/type", "ingress");
                if (org.apache.commons.collections.CollectionUtils.isEmpty(ingressControllers)) {
                    return;
                }
            }
        }
        for (DaemonSet ingressController : ingressControllers) {
            List<Container> containers = ingressController.getSpec().getTemplate().getSpec().getContainers();
            if (org.apache.commons.collections.CollectionUtils.isNotEmpty(containers)) {
                Container container = containers.get(0);
                for (String arg : container.getArgs()) {
                    if (!StringUtils.endsWith(arg, cmName)) {
                        continue;
                    }
                    List<ContainerPort> delContainerPorts = Lists.newArrayList();
                    for (ContainerPort containerPort : container.getPorts()) {
                        if (containerPort.getHostPort() != null && delHostPorts.contains(String.valueOf(containerPort.getHostPort()))) {
                            delContainerPorts.add(containerPort);
                        }
                    }
                    container.getPorts().removeAll(delContainerPorts);
                    ingressController.getSpec().getTemplate().getSpec().getContainers().set(0, container);
                    k8sDaemonSetOperator.createOrReplace(cmNamespace, ingressController);
                    return;
                }
            }
        }
    }


    public String getK8sKind(String name, String namespace) {
        log.info("get => name:{},namespace:{}", name, namespace);
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        Deployment deployment = client.apps().deployments().inNamespace(namespace).withName(name).get();
        if (deployment != null) {
            return K8sKindConstant.DEPLOY;
        }
        StatefulSet statefulSet = client.apps().statefulSets().inNamespace(namespace).withName(name).get();
        client.apps().statefulSets().inNamespace(namespace).withName(name).get();
        if (statefulSet != null) {
            return K8sKindConstant.STATEFULSET;
        }
        DaemonSet daemonSet = client.apps().daemonSets().inNamespace(namespace).withName(name).get();
        if (daemonSet != null) {
            return K8sKindConstant.DS;
        }
        return null;
    }


}