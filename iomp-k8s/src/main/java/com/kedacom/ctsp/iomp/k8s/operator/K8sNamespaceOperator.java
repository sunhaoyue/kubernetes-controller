package com.kedacom.ctsp.iomp.k8s.operator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import com.kedacom.ctsp.iomp.k8s.common.cache.K8sAbstractOperator;
import com.kedacom.ctsp.iomp.k8s.common.util.K8sMapUtil;
import com.kedacom.ctsp.iomp.k8s.constant.K8sConstants;
import com.kedacom.ctsp.iomp.k8s.vo.NsAttributeVo;
import com.kedacom.ctsp.iomp.k8s.vo.NsParamVo;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyIngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyIngressRuleBuilder;
import io.fabric8.kubernetes.api.model.rbac.*;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

import static com.kedacom.ctsp.iomp.k8s.constant.K8sConstants.ENABLED;
import static com.kedacom.ctsp.iomp.k8s.enmu.NamespaceAccessEnum.PRIVATE;
import static com.kedacom.ctsp.iomp.k8s.enmu.NamespaceAccessEnum.PUBLIC;

/**
 * @author Administrator
 */
@Slf4j
public class K8sNamespaceOperator extends K8sAbstractOperator {

    public K8sNamespaceOperator(K8sClientHolder k8sClientHolder) {
        super(k8sClientHolder);
    }

    public static final String LABEL_REGION = "dolphin.region/name";
    public static final String CLUSTER_LEVEL = "dolphin/service-level";
    public static final String NAMESPACE_ACCESS = "access";
    public static final String NETWORK_POLICY_PREFIX = "-network-policy";
    public static final String ANN_SHOW_NAME = "dolphin/show-name";
    public static final String ANN_DESCRIPTION = "dolphin/description";
    public static final String ANN_DEPT_NAME = "dolphin/department-name";
    public static final String LABEL_DEPARTMETN = "dolphin/department";
    public static final String LABEL_NAMESPACE_LXCFS = "dolphin/lxcfs-admission-webhook";
    public static final String DEAFULT_DP_SYSTEM = "dp-system";
    public static final String DEFAULT_NS = "default";
    public static final String KEDACOM_NS = "kedacom-project-namespace";
    public static final String KUBE_PUBLIC_NS = "kube-public";
    public static final String KUBE_SYSTEM_NS = "kube-system";

    public void createOrReplaceNamespace(String name, String access, String level, String showName, String description, String timezone, NsParamVo nsParamVo,
                                         NsAttributeVo attributeVo) {
        log.info("update => name:{},access:{},level:{},showName:{},description:{},timezone:{},department:{},department:{}", name, access, level, showName, description, timezone, JSON.toJSONString(attributeVo));
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        Namespace namespace = client.namespaces().withName(name).get();
        namespace = namespace != null ? namespace : new NamespaceBuilder().withNewMetadata().withName(name).endMetadata().build();

        Map<String, String> annotations = namespace.getMetadata().getAnnotations();
        annotations = annotations != null ? annotations : Maps.newHashMap();


        if (StringUtils.isEmpty(showName)) {
            annotations.remove(ANN_SHOW_NAME);
        } else {
            annotations.put(ANN_SHOW_NAME, showName);
        }
        if (StringUtils.isEmpty(description)) {
            annotations.remove(ANN_DESCRIPTION);
        } else {
            annotations.put(ANN_DESCRIPTION, description);
        }
        if (StringUtils.isEmpty(timezone)) {
            annotations.remove(K8sConstants.NS_TIMEZONE);
        } else {
            annotations.put(K8sConstants.NS_TIMEZONE, timezone);
        }

        String attribute = JSONObject.toJSONString(attributeVo);
        annotations.put(K8sConstants.ANN_ATTRIBUTES, attribute);
        Map<String, String> labels = namespace.getMetadata().getLabels();
        labels = labels != null ? labels : Maps.newHashMap();
        labels.put(NAMESPACE_ACCESS, access);
        if (StringUtils.isEmpty(level)) {
            labels.remove(CLUSTER_LEVEL);
        } else {
            labels.put(CLUSTER_LEVEL, level);
        }
        labels.put(LABEL_DEPARTMETN, nsParamVo.getDepartment());
        K8sMapUtil.put(annotations, ANN_DEPT_NAME, nsParamVo.getDepartmentName(), false);
        if (StringUtils.equalsAny(name, DEAFULT_DP_SYSTEM, DEFAULT_NS, KEDACOM_NS, KUBE_PUBLIC_NS, KUBE_SYSTEM_NS)) {
            labels.remove(LABEL_NAMESPACE_LXCFS);
        } else {
            labels.put(LABEL_NAMESPACE_LXCFS, ENABLED);
        }

        namespace.getMetadata().setLabels(labels);
        namespace.getMetadata().setAnnotations(annotations);
        Long startTime = System.currentTimeMillis();
        client.namespaces().createOrReplace(namespace);
        // 更新namespace
        this.patchCache(namespace);

        createServiceAccountAuth(name);

        String policyName = name + NETWORK_POLICY_PREFIX;
        NetworkPolicy networkPolicy = client.inNamespace(name).network().networkPolicies().withName(policyName).get();
        startTime = System.currentTimeMillis();
        if (StringUtils.equals(PUBLIC.getLabel(), access)) {
            if (networkPolicy != null) {
                // 因为是namespace缓存，所以该对象无需缓存
                client.inNamespace(name).network().networkPolicies().delete(networkPolicy);
            }
        } else if (StringUtils.equals(PRIVATE.getLabel(), access)) {
            if (networkPolicy == null) {
                NetworkPolicyIngressRule ingressRule = new NetworkPolicyIngressRuleBuilder()
                        .addNewFrom()
                        .withNewPodSelector()
                        .endPodSelector()
                        .endFrom()
                        .build();
                networkPolicy = new NetworkPolicyBuilder()
                        .withApiVersion("networking.k8s.io/v1")
                        .withNewMetadata()
                        .withName(policyName)
                        .endMetadata()
                        .withNewSpec()
                        .withNewPodSelector()
                        .endPodSelector()
                        .withPolicyTypes("Ingress")
                        .addToIngress(ingressRule)
                        .endSpec()
                        .build();
                if (log.isDebugEnabled()) {
                    log.debug("networkPolicy: " + JSON.toJSONString(networkPolicy));
                }
                // 因为是namespace缓存，所以该对象无需更新缓存
                client.inNamespace(name).network().networkPolicies().createOrReplace(networkPolicy);
            }
        }
    }

    public void patch(String name, String access, String level, String showName, String description, String timezone, NsParamVo nsParamVo,
                      NsAttributeVo attributeVo) {
        log.info("update => name:{},access:{},level:{},showName:{},description:{},timezone:{},department:{},department:{}", name, access, level, showName, description, timezone, JSON.toJSONString(attributeVo));
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        Namespace namespace = client.namespaces().withName(name).get();
        namespace = namespace != null ? namespace : new NamespaceBuilder().withNewMetadata().withName(name).endMetadata().build();

        Map<String, String> annotations = namespace.getMetadata().getAnnotations();
        annotations = annotations != null ? annotations : Maps.newHashMap();


        if (StringUtils.isNotBlank(showName)) {
            annotations.put(ANN_SHOW_NAME, showName);
        }
        if (StringUtils.isNotBlank(description)) {
            annotations.put(ANN_DESCRIPTION, description);
        }
        if (StringUtils.isNotBlank(timezone)) {
            annotations.put(K8sConstants.NS_TIMEZONE, timezone);
        }

        if(attributeVo != null){
            String attributeOld = annotations.get(K8sConstants.ANN_ATTRIBUTES);
            if(StringUtils.isNotBlank(attributeOld)){
                JSONObject jsonObject = JSON.parseObject(attributeOld);
                if(attributeVo.getDownload() == null ){
                    if(jsonObject.get("download") != null){
                        attributeVo.setDownload((String) jsonObject.get("download"));
                    }else {
                        attributeVo.setDownload(K8sConstants.ENABLED);
                    }
                }
                if(attributeVo.getTerminal() == null){
                    if(jsonObject.get("terminal") != null){
                        attributeVo.setTerminal((String) jsonObject.get("terminal"));
                    }else {
                        attributeVo.setTerminal(K8sConstants.ENABLED);
                    }
                }
            }

            String attribute = JSONObject.toJSONString(attributeVo);
            annotations.put(K8sConstants.ANN_ATTRIBUTES, attribute);
        }

        Map<String, String> labels = namespace.getMetadata().getLabels();
        labels = labels != null ? labels : Maps.newHashMap();

        if (StringUtils.isNotBlank(access)) {
            labels.put(NAMESPACE_ACCESS, access);
        }
        if (StringUtils.isNotBlank(level)) {
            labels.put(CLUSTER_LEVEL, level);
        }

        if (nsParamVo != null) {
            labels.put(LABEL_DEPARTMETN, nsParamVo.getDepartment());
            K8sMapUtil.put(annotations, ANN_DEPT_NAME, nsParamVo.getDepartmentName(), false);
            if (StringUtils.equalsAny(name, DEAFULT_DP_SYSTEM, DEFAULT_NS, KEDACOM_NS, KUBE_PUBLIC_NS, KUBE_SYSTEM_NS)) {
                labels.remove(LABEL_NAMESPACE_LXCFS);
            } else {
                labels.put(LABEL_NAMESPACE_LXCFS, ENABLED);
            }
        }


        namespace.getMetadata().setLabels(labels);
        namespace.getMetadata().setAnnotations(annotations);
        Long startTime = System.currentTimeMillis();
        client.namespaces().createOrReplace(namespace);
        // 更新namespace
        this.patchCache(namespace);

        createServiceAccountAuth(name);

        String policyName = name + NETWORK_POLICY_PREFIX;
        NetworkPolicy networkPolicy = client.inNamespace(name).network().networkPolicies().withName(policyName).get();
        startTime = System.currentTimeMillis();
        if(StringUtils.isNotBlank(access)){
            if (StringUtils.equals(PUBLIC.getLabel(), access)) {
                if (networkPolicy != null) {
                    // 因为是namespace缓存，所以该对象无需缓存
                    client.inNamespace(name).network().networkPolicies().delete(networkPolicy);
                }
            } else if (StringUtils.equals(PRIVATE.getLabel(), access)) {
                if (networkPolicy == null) {
                    NetworkPolicyIngressRule ingressRule = new NetworkPolicyIngressRuleBuilder()
                            .addNewFrom()
                            .withNewPodSelector()
                            .endPodSelector()
                            .endFrom()
                            .build();
                    networkPolicy = new NetworkPolicyBuilder()
                            .withApiVersion("networking.k8s.io/v1")
                            .withNewMetadata()
                            .withName(policyName)
                            .endMetadata()
                            .withNewSpec()
                            .withNewPodSelector()
                            .endPodSelector()
                            .withPolicyTypes("Ingress")
                            .addToIngress(ingressRule)
                            .endSpec()
                            .build();
                    if (log.isDebugEnabled()) {
                        log.debug("networkPolicy: " + JSON.toJSONString(networkPolicy));
                    }
                    // 因为是namespace缓存，所以该对象无需更新缓存
                    client.inNamespace(name).network().networkPolicies().createOrReplace(networkPolicy);
                }
            }
        }

    }

    public void apiCreateOrReplaceNamespace(String name, String access, String level, String showName, String description, String timezone, String department,
                                            NsAttributeVo attributeVo) {
        log.info("update => name:{},access:{},level:{},showName:{},description:{},timezone:{},department:{},department:{}", name, access, level, showName, description, timezone, JSON.toJSONString(attributeVo));
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        Namespace namespace = client.namespaces().withName(name).get();
        namespace = namespace != null ? namespace : new NamespaceBuilder().withNewMetadata().withName(name).endMetadata().build();

        Map<String, String> annotations = namespace.getMetadata().getAnnotations();
        annotations = annotations != null ? annotations : Maps.newHashMap();


        if (null != showName) {
            if (StringUtils.isEmpty(showName)) {
                annotations.remove(ANN_SHOW_NAME);
            } else {
                annotations.put(ANN_SHOW_NAME, showName);
            }
        }
        if (null != description) {
            if (StringUtils.isEmpty(description)) {
                annotations.remove(ANN_DESCRIPTION);
            } else {
                annotations.put(ANN_DESCRIPTION, description);
            }
        }
        if (null != timezone) {
            if (StringUtils.isEmpty(timezone)) {
                annotations.remove(K8sConstants.NS_TIMEZONE);
            } else {
                annotations.put(K8sConstants.NS_TIMEZONE, timezone);
            }
        }

        String attribute = JSONObject.toJSONString(attributeVo);
        annotations.put(K8sConstants.ANN_ATTRIBUTES, attribute);
//        if (StringUtils.isEmpty(regionSigns)) {
//            annotations.remove(K8sConstants.NS_REGIONS);
//        } else {
//            annotations.put(K8sConstants.NS_REGIONS, regionSigns);
//        }


        Map<String, String> labels = namespace.getMetadata().getLabels();
        labels = labels != null ? labels : Maps.newHashMap();

        labels.put(NAMESPACE_ACCESS, access);
//        if (StringUtils.isEmpty(regionSign)) {
//            labels.remove(LABEL_REGION);
//        } else {
//            labels.put(LABEL_REGION, regionSign);
//        }
        if (null != level) {
            if (StringUtils.isEmpty(level)) {
                labels.remove(CLUSTER_LEVEL);
            } else {
                labels.put(CLUSTER_LEVEL, level);
            }
        }
        if (null != department) {
            labels.put(LABEL_DEPARTMETN, department);
        }
        namespace.getMetadata().setLabels(labels);
        namespace.getMetadata().setAnnotations(annotations);
        Long startTime = System.currentTimeMillis();
        client.namespaces().createOrReplace(namespace);

        createServiceAccountAuth(name);

        String policyName = name + NETWORK_POLICY_PREFIX;
        NetworkPolicy networkPolicy = client.inNamespace(name).network().networkPolicies().withName(policyName).get();
        startTime = System.currentTimeMillis();
        if (StringUtils.equals(PUBLIC.getLabel(), access)) {
            if (networkPolicy != null) {
                client.inNamespace(name).network().networkPolicies().delete(networkPolicy);
            }
        } else if (StringUtils.equals(PRIVATE.getLabel(), access)) {
            if (networkPolicy == null) {
                NetworkPolicyIngressRule ingressRule = new NetworkPolicyIngressRuleBuilder()
                        .addNewFrom()
                        .withNewPodSelector()
                        .endPodSelector()
                        .endFrom()
                        .build();
                networkPolicy = new NetworkPolicyBuilder()
                        .withApiVersion("networking.k8s.io/v1")
                        .withNewMetadata()
                        .withName(policyName)
                        .endMetadata()
                        .withNewSpec()
                        .withNewPodSelector()
                        .endPodSelector()
                        .withPolicyTypes("Ingress")
                        .addToIngress(ingressRule)
                        .endSpec()
                        .build();
                if (log.isDebugEnabled()) {
                    log.info("networkPolicy: " + JSON.toJSONString(networkPolicy));
                }
                client.inNamespace(name).network().networkPolicies().createOrReplace(networkPolicy);
            }
        }
    }


    public void createServiceAccount(String namespace, String serviceAccountName) {
        log.info("update => namespace:{},serviceAccountName:{}", namespace, serviceAccountName);
        if (k8sClientHolder.getClient().serviceAccounts().inNamespace(namespace).withName(serviceAccountName).get() != null) {
            return;
        }
        ServiceAccountBuilder serviceAccountBuilder = new ServiceAccountBuilder();
        ServiceAccount serviceAccount = serviceAccountBuilder.withApiVersion("v1")
                .withNewMetadata().withName(serviceAccountName).withNamespace(namespace).endMetadata().build();
        if (log.isDebugEnabled()) {
            log.debug("networkPolicy: " + JSON.toJSONString(serviceAccount));
        }
        // 因为是namespace缓存，所以该对象无需更新缓存
        k8sClientHolder.getClient().serviceAccounts().inNamespace(namespace).createOrReplace(serviceAccount);

    }

    public void createRole(String namespace, String roleName) {
        log.info("update => namespace:{},roleName:{}", namespace, roleName);
        if (k8sClientHolder.getClient().rbac().roles().inNamespace(namespace).withName(roleName).get() != null) {
            return;
        }
        RoleBuilder builder = new RoleBuilder();
        ClusterRole clusterRole = k8sClientHolder.getClient().rbac().clusterRoles().withName(roleName).get();
        List<PolicyRule> rules = clusterRole.getRules();
        builder.withApiVersion(K8sConstants.API_VERSION_CLUSTER_ROLE)
                .withNewMetadata().withName(roleName).withNamespace(namespace).endMetadata()
                .addAllToRules(rules);
        if (log.isDebugEnabled()) {
            log.debug("Role: " + JSON.toJSONString(builder.build()));
        }
        // 因为是namespace缓存，所以该对象无需更新缓存
        log.info("当前client的namespace为=>{}", k8sClientHolder.getClient().getNamespace());

        k8sClientHolder.getClient().rbac().roles().inNamespace(namespace).createOrReplace(builder.build());

    }

    public void createRoleBinding(String namespace, String serviceAccountName, String roleName) {
        log.info("update => namespace:{},serviceAccountName:{},roleName:{}", namespace, serviceAccountName, roleName);
        String roleBindingName = serviceAccountName + "_" + namespace;
        if (k8sClientHolder.getClient().rbac().roleBindings().inNamespace(namespace).withName(roleBindingName).get() != null) {
            return;
        }
        RoleBindingBuilder builder = new RoleBindingBuilder();
        builder.withApiVersion(K8sConstants.API_VERSION_CLUSTER_ROLE)
                .withNewMetadata().withName(roleBindingName).withNamespace(namespace).endMetadata()
                .withSubjects(new Subject(null, "ServiceAccount", serviceAccountName, namespace))
                .withRoleRef(new RoleRef(K8sConstants.API_GROUP_CLUSTER_ROLE, "Role", roleName));
        if (log.isDebugEnabled()) {
            log.debug("RoleBinding: " + JSON.toJSONString(builder.build()));
        }

        k8sClientHolder.getClient().rbac().roleBindings().inNamespace(namespace).createOrReplace(builder.build());

    }

    public void createClusterRoleBinding(String namespace, String serviceAccountName, String clusterRoleName) {
        log.info("update => namespace:{},serviceAccountName:{},clusterRoleName:{}", namespace, serviceAccountName, clusterRoleName);
        String clusterRoleBindingName = serviceAccountName + "_" + namespace;
        ClusterRoleBinding clusterRoleBinding = k8sClientHolder.getClient().rbac().clusterRoleBindings().withName(clusterRoleBindingName).get();
        //如果一致的话，就不需要更改
        if (clusterRoleBinding != null && StringUtils.equals(clusterRoleName, clusterRoleBinding.getRoleRef().getName())) {
            return;
        }

        ClusterRoleBindingBuilder builder = new ClusterRoleBindingBuilder();
        builder.withApiVersion(K8sConstants.API_VERSION_CLUSTER_ROLE)
                .withNewMetadata().withName(clusterRoleBindingName).withNamespace(namespace).endMetadata()
                .withSubjects(new Subject(null, "ServiceAccount", serviceAccountName, namespace))
                .withRoleRef(new RoleRef(K8sConstants.API_GROUP_CLUSTER_ROLE, "ClusterRole", clusterRoleName));
        k8sClientHolder.getClient().rbac().clusterRoleBindings().withName(clusterRoleBindingName).delete();
        if (log.isDebugEnabled()) {
            log.debug("ClusterRoleBinding: " + JSON.toJSONString(builder.build()));
        }
        // 因为是namespace缓存，所以该对象无需更新缓存
        k8sClientHolder.getClient().inNamespace(namespace).rbac().clusterRoleBindings().createOrReplace(builder.build());
    }


    public void deleteNamespace(String name) {
        log.info("delete => name:{}", name);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        client.namespaces().withName(name).delete();
        this.delCache(name, null);
    }

    public Boolean deleteWithLabelIn(String key, String... values) {
        log.info("delete => namespace:{},key:{},values:{}", key, values);
        boolean result;
        result = k8sClientHolder.getClient().namespaces().withLabelIn(key, values).delete();
        if (result) {
            this.delCacheByLabel(null, key, values);
        }
        return result;
    }


    public Namespace getNamespaceByName(String name) {
        log.info("get => name:{}", name);
        return k8sClientHolder.getClient().namespaces().withName(name).get();
    }

    public List<Namespace> getAllNamespace() {
        return k8sClientHolder.getClient().namespaces().list().getItems();
    }


    public List<Namespace> getByLabel(String key, String value) {
        log.info("get => name:{},value:{}", key, value);
        return k8sClientHolder.getClient().namespaces().withLabel(key, value).list().getItems();
    }

    public List<Namespace> getWithLabelExsit(List<String> labels) {
        List<LabelSelectorRequirement> requirements = Lists.newArrayList();
        for (String label : labels) {
            requirements.add(new LabelSelectorRequirementBuilder()
                    .withKey(label).withOperator("Exists").build());
        }
        return k8sClientHolder.getClient().namespaces().withLabelSelector(new LabelSelectorBuilder().withMatchExpressions(requirements).build()).list().getItems();
    }



    public int getNamespaceServiceAmount(String namespaceName) {
        log.info("get => namespaceName:{}", namespaceName);
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        List<Deployment> podList = client.apps().deployments().inNamespace(namespaceName).list().getItems();
        List<DaemonSet> daemonSetList = client.apps().daemonSets().inNamespace(namespaceName).list().getItems();
        List<StatefulSet> statefulSetList = client.apps().statefulSets().inNamespace(namespaceName).list().getItems();
        return podList.size() + daemonSetList.size() + statefulSetList.size();
    }

    public void createViewConfigmapRole(String namespace, String roleName) {
        log.info("update => namespace:{},roleName:{}", namespace, roleName);
        if (k8sClientHolder.getClient().rbac().roles().inNamespace(namespace).withName(roleName).get() != null) {
            return;
        }
        RoleBuilder builder = new RoleBuilder();
        PolicyRule policyRule = new PolicyRuleBuilder()
                .withApiGroups("")
                .withResources("configmaps")
                .withVerbs(Lists.newArrayList("get", "list", "watch"))
                .build();
        builder.withApiVersion(K8sConstants.API_VERSION_CLUSTER_ROLE)
                .withNewMetadata().withName(roleName).withNamespace(namespace).endMetadata()
                .addAllToRules(Lists.newArrayList(policyRule));
        if (log.isDebugEnabled()) {
            log.debug("Role => " + JSON.toJSONString(builder.build()));
        }
        k8sClientHolder.getClient().rbac().roles().inNamespace(namespace).createOrReplace(builder.build());
    }


    /**
     * 创建需要的serviceaccount的一整个体系
     *
     * @param namespace
     */
    public void createServiceAccountAuth(String namespace) {
        createServiceAccount(namespace, "admin");
        createServiceAccount(namespace, "cluster-read");
        createServiceAccount(namespace, "ns-admin");
        createServiceAccount(namespace, "ns-read");
        createRole(namespace, "admin");
        createRole(namespace, "view");
        createViewConfigmapRole(namespace, "view-configmap");
        createRoleBinding(namespace, "ns-admin", "admin");
        createRoleBinding(namespace, "ns-read", "view");
        createRoleBinding(namespace, "default", "view-configmap");
//        createClusterRoleBinding(namespace, "admin", "admin");
        createClusterRoleBinding(namespace, "admin", "cluster-admin");
        createClusterRoleBinding(namespace, "cluster-read", "view");
    }

    public void createOrReplace(Namespace namespace) {
        log.info("update => namespace:{}", namespace);
        k8sClientHolder.getClient().namespaces().createOrReplace(namespace);
        this.patchCache(namespace);
    }

    public List<Namespace> withLabelIn(String key, String... values) {
        log.info("get => key:{},values:{}", key, values);
        return k8sClientHolder.getClient().namespaces().withLabelIn(key, values).list().getItems();
    }


    /**
     * 获取空间上绑定的网关和域名
     *
     * @param namespace
     * @return
     */
    public Map<String, List<String>> getNetwork(String namespace) {
        log.info("get => namespace:{}", namespace);
        if (StringUtils.isEmpty(namespace)) {
            return Maps.newHashMap();
        }
        Namespace ns = getNamespaceByName(namespace);
        Map<String, List<String>> result = Maps.newHashMap();
        List<String> gatewayList = Lists.newArrayList();
        List<String> tcpGatewayList = Lists.newArrayList();

        if (ns != null && ns.getMetadata().getAnnotations() != null) {
            String gateways = ns.getMetadata().getAnnotations().get(K8sConstants.NS_GATEWAY);
            String tcpgateways = ns.getMetadata().getAnnotations().get(K8sConstants.NS_TCP_GATEWAY);
            if (StringUtils.isNotEmpty(gateways)) {
                String[] args = StringUtils.split(gateways, ",");
                for (String arg : args) {
                    gatewayList.add(arg.trim());
                }
            }
            if (StringUtils.isNotEmpty(tcpgateways)) {
                String[] args = StringUtils.split(tcpgateways, ",");
                for (String arg : args) {
                    tcpGatewayList.add(arg.trim());
                }
            }
        }
        result.put(K8sConstants.NS_GATEWAY, gatewayList);
        result.put(K8sConstants.NS_TCP_GATEWAY, tcpGatewayList);
        return result;
    }


    @Override
    public List<Namespace> list() {
        log.info("list all");
        return k8sClientHolder.getClient().namespaces().list().getItems();
    }

    @Override
    public List<Namespace> list(String namespace) {
        return this.list();
    }


    @Override
    public Namespace toSimpleData(HasMetadata hasMetadata) {
        Namespace namespace = (Namespace) hasMetadata;
        return namespace;
    }
}
