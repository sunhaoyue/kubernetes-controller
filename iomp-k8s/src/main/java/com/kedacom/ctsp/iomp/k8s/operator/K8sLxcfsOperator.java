package com.kedacom.ctsp.iomp.k8s.operator;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import com.kedacom.ctsp.iomp.k8s.constant.K8sConstants;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.*;
import io.fabric8.kubernetes.api.model.apps.*;
import io.fabric8.kubernetes.api.model.certificates.v1beta1.CertificateSigningRequest;
import io.fabric8.kubernetes.api.model.certificates.v1beta1.CertificateSigningRequestBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class K8sLxcfsOperator {

    public static final String LXCFS_NAMESPACE = "dp-system";

    public final String LXCFS_DAEMONSET_NAME = "lxcfs";

    public static final String LXCFS_CSR_NAME = "lxcfs-admission-webhook-svc.dp-system";

    public final String LXCFS_CSR_REQUEST = "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURSBSRVFVRVNULS0tLS0KTUlJRE5UQ0NBaDBDQVFBd05ERXlNREFHQTFVRUF3d3BiSGhqWm5NdFlXUnRhWE56YVc5dUxYZGxZbWh2YjJzdApjM1pqTG1Sd0xYTjVjM1JsYlM1emRtTXdnZ0VpTUEwR0NTcUdTSWIzRFFFQkFRVUFBNElCRHdBd2dnRUtBb0lCCkFRQ3ZtSURhblRNWlo5d3h2bFJ5dnppY2QzazlLeURSZmplVGRsakVqSmh4alJ1K3dJZWFqSzlWVHhHcGNjLzMKejJacVBrNTRvWmNtYk5GT1N5RU5BUWRrK2EzY2xEZjgxdVB3RjU0OXhqeEg2WXVPbDlibG90MTFzdm9BMmVzQwpOWkhJL3Rsa1hBbS9JZDk2czlEUWtRaGZlK2pkL1d0cE1NOVR6RkoxR0RTRDQxQXYxYUF3dW1MVktYTTJ1VE1WCk55RUNpaGVDdzM0T0gxc0c1cVZwLzY0NFBtNUtXUVhzcGUyYmc2SjZ5N2pzTTZZdytqZTBiUGo4YUI3OXZmbHUKOC9tUjh0VXpXNVBvTEhCWWdmU1dtNEFDclVHM05qQy9PZlRrNFJYanJUWHRlWVBxOVZ2TUNlb3NsSFQwZDdyOQpBRERrY1lwUkl2WFNyZENCelRnRW9JYzFBZ01CQUFHZ2dic3dnYmdHQ1NxR1NJYjNEUUVKRGpHQnFqQ0JwekFKCkJnTlZIUk1FQWpBQU1Bc0dBMVVkRHdRRUF3SUY0REFUQmdOVkhTVUVEREFLQmdnckJnRUZCUWNEQVRCNEJnTlYKSFJFRWNUQnZnaHRzZUdObWN5MWhaRzFwYzNOcGIyNHRkMlZpYUc5dmF5MXpkbU9DSld4NFkyWnpMV0ZrYldsegpjMmx2YmkxM1pXSm9iMjlyTFhOMll5NWtjQzF6ZVhOMFpXMkNLV3g0WTJaekxXRmtiV2x6YzJsdmJpMTNaV0pvCmIyOXJMWE4yWXk1a2NDMXplWE4wWlcwdWMzWmpNQTBHQ1NxR1NJYjNEUUVCQ3dVQUE0SUJBUUFWVEJhZlNBa1UKZWJtYVowL2RZRGNUamZJSWZIUFMyOFdHK3gvc2R1dW42LzBzOUpqK1B5a0gvbVBjdTZqRU5qRERvaU9FamxkeApyalAvSWZ4dktOdE95ZXd0UjYyVHRwRExHRUZ2RExoblNYOVlGVFAwOWMrVzBYbTBLbndkczZtVEw5eVROQi9rCjh5R2hoUzE2WSthWGhOS3A3eWlKVWdldkMzdUYvSThjaWZzaGNFdzNxQ0llTUxvQnRiY2RDd0RoVjg1eWgxenQKalRHNDE1QnlJZWhzRTc0N2RKY3BrQWhITFNSdnJWcTJiazNBNnVKN0J6cE5IL0VYWCtDQTRQQ2l5bVYvVGVUNQpOVnlkTlJaVEhjNHJwK2dLYjQ0TWhPaFMxUVpZQWp0WG4ya2hCaEJnYnZaQkpXTjMvSENHcjFkQktoNnlRVzVkCnFzNmZtUkl4eEVlYgotLS0tLUVORCBDRVJUSUZJQ0FURSBSRVFVRVNULS0tLS0K";

    public final String LXCFS_CSR_SIGNER = "kubernetes.io/kubelet-serving";

    public final String LXCFS_SECRET_NAME = "lxcfs-admission-webhook-certs";

    public final String LXCFS_SERVICE_NAME = "lxcfs-admission-webhook-svc";

    public final String LXCFS_DEPLOY_NAME = "lxcfs-admission-webhook";

    public final String LXCFS_CONFIGURATION_NAME = "mutating-lxcfs-admission-webhook-cfg";

    private final K8sClientHolder k8sClientHolder;

    public K8sLxcfsOperator(K8sClientHolder k8sClientHolder) {
        this.k8sClientHolder = k8sClientHolder;
    }

    /**
     * 创建lxcfs
     * 默认在dp-system空间下
     */
    public void createLxcfs(String registryUrl) {

        // 1. daemonset
        createLxcfsDaemonset(registryUrl);
        // 2. csr
        createLxcfsCsr();
        // 3. secret
        createLxcfsSecret();
        // 4. service
        createLxcfsService();
        // 5. deploy
        createLxcfsDeploy(registryUrl);
        // 6. configuration
        createLxcfsConfiguration();

    }

    /**
     * configuration
     */
    public void createLxcfsConfiguration() {
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        MutatingWebhookConfiguration cfg = client.admissionRegistration().v1().mutatingWebhookConfigurations().withName(LXCFS_CONFIGURATION_NAME).get();
        if (cfg != null) {
            return;
        }
        Map<String, String> label = Maps.newHashMap();
        label.put("app.kubernetes.io/component", LXCFS_DEPLOY_NAME);
        label.put("app.kubernetes.io/instance", LXCFS_DEPLOY_NAME);
        label.put("app.kubernetes.io/name", LXCFS_DEPLOY_NAME);

        Map<String, String> namespaceSelector = Maps.newHashMap();
        namespaceSelector.put("dolphin/lxcfs-admission-webhook", "enabled");

        MutatingWebhook webhook = new MutatingWebhookBuilder()
                .withAdmissionReviewVersions("v1beta1")
                .withNewClientConfig()
                .withCaBundle("LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUM2VENDQWRHZ0F3SUJBZ0lCQURBTkJna3Foa2lHOXcwQkFRc0ZBREFWTVJNd0VRWURWUVFERXdwcmRXSmwKY201bGRHVnpNQ0FYRFRJeU1ETXlNakV6TXpBek9Wb1lEekl4TWpJd01qSTJNVE16TURNNVdqQVZNUk13RVFZRApWUVFERXdwcmRXSmxjbTVsZEdWek1JSUJJakFOQmdrcWhraUc5dzBCQVFFRkFBT0NBUThBTUlJQkNnS0NBUUVBCjJzbWxSR2o0N2xuM1FJYjBQaXNJZ2lhczh0NlQrMCtJQ3hNTkt3YkVPQzBrbnRYSytXcXFZc001SWlER1hSWHQKYlNTNlgySDlUQXozNFZ2ZVpvTHpCdFE2T0NhRkdLUWNySCtyS1hOU0FFdDZTZ3oyQXdNZDdhU1crZ2YvZDJObgpyUzhDZ0ZVZHNsREVoZTFPNjN4Vjc1elJKQVMwRDVBdCtnM3UzMG9XcjRZODR2NUxnQWFWZFhGa0QzWTM2b0E3CkRmSjFHeElGcU8vYkdNeWdhVlB5cEs4NnZYbWoxMlcwMmxqR29Xdkdodncvb3lUdXRUelVqSUhnbXdEZ3h2bzMKQUxzUnZ5MkxHb2F3UDgzN3lFQis2Q0x2dWloZk8yV1piWk0raEZDS3djM1F6anFEelRYZHpnSUVWMzZQV0lURwo2bnd2Q1FrcWlFdFR0SXR1aGd1dll3SURBUUFCbzBJd1FEQU9CZ05WSFE4QkFmOEVCQU1DQXFRd0R3WURWUjBUCkFRSC9CQVV3QXdFQi96QWRCZ05WSFE0RUZnUVVXbEtERlRHdXA2dnRNTWw1enFCUk9ua1U2ek13RFFZSktvWkkKaHZjTkFRRUxCUUFEZ2dFQkFDS2JYMlNSYUhLSVpuSmgxNHhucjVmeURtRG9VcHdYRjdkbG9pOWFjWXZLRUZMcAp4N2wyRjJET2QvV3k1UzVoc242L3VGWGE4OUQxSDAwTXl4cFRpM2taZFh6QUFlUjFMZ1QvWXhyRDMrVWhMQk50ClVmRVd4V2J5TmNIRXRsaE1IUkFqeUFYR09vWWVMUTlrNStnZkRyRCtFOW1xemMzZE40WnFGaTY2RjlEalNSa0YKdVdlQ1M3djJJN0s4RjdWNDUyZm1MZkl0c3JWc0R5Tk5QVW1oOVlUUGZUM2hrTUlmOUF0MFk4VWRHK0dNb1hxYgptTGZlL2o5WjVUVTVLOXhRaGJqb204d2lVaUlEUWw1dlg2UEpwZGRobVRvZnpkclhjMWpPQmpoYXF4c0JXc1JECmJTWHR4dXkyb2EraitCeHZiYjZTb3FPdXVOUnhYNURuU29GVlkraz0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQo=")
                .withNewService(LXCFS_SERVICE_NAME, LXCFS_NAMESPACE, "/mutate", 443)
                .endClientConfig()
                .withFailurePolicy("Ignore")
                .withMatchPolicy("Exact")
                .withName("mutating.lxcfs-admission-webhook.aliyun.com")
                .withNewNamespaceSelector()
                .withMatchLabels(namespaceSelector)
                .endNamespaceSelector()
                .withReinvocationPolicy("Never")
                .withSideEffects("None")
                .withTimeoutSeconds(30)
                .withRules(new RuleWithOperationsBuilder()
                        .withApiGroups(Lists.newArrayList("core", ""))
                        .withApiVersions("v1")
                        .withOperations("CREATE")
                        .withResources("pods")
                        .withScope("*")
                        .build())
                .build();

        MutatingWebhookConfiguration build = new MutatingWebhookConfigurationBuilder()
                .withNewMetadata()
                .withLabels(label)
                .withName(LXCFS_CONFIGURATION_NAME)
                .endMetadata()
                .withWebhooks(webhook)
                .build();
        client.admissionRegistration().v1().mutatingWebhookConfigurations().createOrReplace(build);
    }

    /**
     * deploy
     *
     * @param registryUrl
     */
    public void createLxcfsDeploy(String registryUrl) {
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        Deployment deployment = client.apps().deployments().inNamespace(LXCFS_NAMESPACE).withName(LXCFS_DEPLOY_NAME).get();
        if (deployment != null) {
            return;
        }
        Map<String, String> label = Maps.newHashMap();
        label.put("app.kubernetes.io/component", LXCFS_DEPLOY_NAME);
        label.put("app.kubernetes.io/instance", LXCFS_DEPLOY_NAME);
        label.put("app.kubernetes.io/name", LXCFS_DEPLOY_NAME);
        label.put("dolphin/service-level", "L1");
        Map<String, String> selector = Maps.newHashMap();
        selector.put("app.kubernetes.io/instance", LXCFS_DEPLOY_NAME);

        Map<String, Quantity> requests = Maps.newHashMap();
        requests.put("cpu", new Quantity("0", null));
        requests.put("memory", new Quantity("0", "Mi"));
        Map<String, Quantity> limits = Maps.newHashMap();
        limits.put("cpu", new Quantity("1", null));
        limits.put("memory", new Quantity("1024", "Mi"));

        DeploymentStrategy rollingUpdate = new DeploymentStrategyBuilder()
                .withType("RollingUpdate")
                .withNewRollingUpdate()
                .withMaxSurge(new IntOrStringBuilder().withStrVal("25%").build())
                .withMaxUnavailable(new IntOrStringBuilder().withStrVal("25%").build())
                .endRollingUpdate()
                .build();

        List<VolumeMount> volumeMounts = Lists.newArrayList();
        volumeMounts.add(new VolumeMountBuilder().withName("webhook-certs").withMountPath("/etc/webhook/certs").withReadOnly(true).build());

        Container container = new ContainerBuilder()
                .withArgs(Lists.newArrayList("-tlsCertFile=/etc/webhook/certs/cert.pem", "-tlsKeyFile=/etc/webhook/certs/key.pem", "-alsologtostderr", "-v=4", "2>&1"))
                .withImagePullPolicy(K8sConstants.IMAGE_PULL_POLICY_RESENT)
                .withImage(registryUrl.concat("/denverdino/lxcfs-admission-webhook:v1"))
                .withName(LXCFS_DEPLOY_NAME)
                .withNewResources()
                .withRequests(requests)
                .withLimits(limits)
                .endResources()
                .withTerminationMessagePath("/dev/termination-log")
                .withTerminationMessagePolicy("File")
                .withVolumeMounts(volumeMounts)
                .build();

        List<Volume> volumes = Lists.newArrayList();
        volumes.add(new VolumeBuilder().withName("webhook-certs").withNewSecret().withSecretName(LXCFS_SECRET_NAME).withDefaultMode(420).endSecret().build());


        Deployment build = new DeploymentBuilder()
                .withNewMetadata()
                .withName(LXCFS_DEPLOY_NAME)
                .withNamespace(LXCFS_NAMESPACE)
                .withLabels(label)
                .endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withNewSelector().withMatchLabels(selector).endSelector()
                .withStrategy(rollingUpdate)
                .withNewTemplate()
                .withNewMetadata()
                .withLabels(selector)
                .endMetadata()
                .withNewSpec()
                .withContainers(container)
                .withDnsPolicy(K8sConstants.CLUSTER_FIRST)
                .withRestartPolicy(K8sConstants.RESTART_POLICY_ALWAYS)
                .withTolerations(new TolerationBuilder().withOperator("Exists").build())
                .withPriorityClassName("dolphin-level-1")
                .withVolumes(volumes)
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
        client.apps().deployments().inNamespace(LXCFS_NAMESPACE).createOrReplace(build);
    }

    /**
     * service
     */
    public void createLxcfsService() {
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        Service service = client.services().inNamespace(LXCFS_NAMESPACE).withName(LXCFS_SERVICE_NAME).get();
        if (service != null) {
            return;
        }
        Map<String, String> label = Maps.newHashMap();
        label.put("app.kubernetes.io/component", LXCFS_DEPLOY_NAME);
        label.put("app.kubernetes.io/instance", LXCFS_DEPLOY_NAME);
        label.put("app.kubernetes.io/name", LXCFS_DEPLOY_NAME);
        Map<String, String> selector = Maps.newHashMap();
        selector.put("app.kubernetes.io/instance", LXCFS_DEPLOY_NAME);
        Service build = new ServiceBuilder()
                .withNewMetadata()
                .withLabels(label)
                .withName(LXCFS_SERVICE_NAME)
                .withNamespace(LXCFS_NAMESPACE)
                .endMetadata()
                .withNewSpec()
                .withPorts(new ServicePortBuilder().withNewTargetPort(443).withPort(443).withProtocol("TCP").build())
                .withSelector(selector)
                .withType(K8sConstants.CLUSTER_IP)
                .endSpec()
                .build();
        client.services().inNamespace(LXCFS_NAMESPACE).createOrReplace(build);
    }

    /**
     * secret
     */
    public void createLxcfsSecret() {
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        Secret secret = client.secrets().inNamespace(LXCFS_NAMESPACE).withName(LXCFS_SECRET_NAME).get();
        if (secret != null) {
            return;
        }
        Map<String, String> label = Maps.newHashMap();
        label.put("app.kubernetes.io/component", LXCFS_DEPLOY_NAME);
        label.put("app.kubernetes.io/instance", LXCFS_DEPLOY_NAME);
        label.put("app.kubernetes.io/name", LXCFS_DEPLOY_NAME);
        Map<String, String> data = Maps.newHashMap();
        data.put("cert.pem", "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURwakNDQW82Z0F3SUJBZ0lQVFp3Wnc0MGR0dVd0L0JrYVBjNE5NQTBHQ1NxR1NJYjNEUUVCQ3dVQU1CVXgKRXpBUkJnTlZCQU1UQ210MVltVnlibVYwWlhNd0lCY05Nakl3TkRFeE1EWTFPVFUxV2hnUE1qRXlNakF5TWpZeApNek13TXpsYU1EUXhNakF3QmdOVkJBTVRLV3g0WTJaekxXRmtiV2x6YzJsdmJpMTNaV0pvYjI5ckxYTjJZeTVrCmNDMXplWE4wWlcwdWMzWmpNSUlCSWpBTkJna3Foa2lHOXcwQkFRRUZBQU9DQVE4QU1JSUJDZ0tDQVFFQXI1aUEKMnAwekdXZmNNYjVVY3I4NG5IZDVQU3NnMFg0M2szWll4SXlZY1kwYnZzQ0htb3l2VlU4UnFYSFA5ODltYWo1TwplS0dYSm16UlRrc2hEUUVIWlBtdDNKUTMvTmJqOEJlZVBjWThSK21ManBmVzVhTGRkYkw2QU5uckFqV1J5UDdaClpGd0p2eUhmZXJQUTBKRUlYM3ZvM2YxcmFURFBVOHhTZFJnMGcrTlFMOVdnTUxwaTFTbHpOcmt6RlRjaEFvb1gKZ3NOK0RoOWJCdWFsYWYrdU9ENXVTbGtGN0tYdG00T2llc3U0N0RPbU1QbzN0R3o0L0dnZS9iMzVidlA1a2ZMVgpNMXVUNkN4d1dJSDBscHVBQXExQnR6WXd2em4wNU9FVjQ2MDE3WG1ENnZWYnpBbnFMSlIwOUhlNi9RQXc1SEdLClVTTDEwcTNRZ2MwNEJLQ0hOUUlEQVFBQm80SFJNSUhPTUE0R0ExVWREd0VCL3dRRUF3SUZvREFUQmdOVkhTVUUKRERBS0JnZ3JCZ0VGQlFjREFUQU1CZ05WSFJNQkFmOEVBakFBTUI4R0ExVWRJd1FZTUJhQUZGcFNneFV4cnFlcgo3VERKZWM2Z1VUcDVGT3N6TUhnR0ExVWRFUVJ4TUcrQ0cyeDRZMlp6TFdGa2JXbHpjMmx2YmkxM1pXSm9iMjlyCkxYTjJZNElsYkhoalpuTXRZV1J0YVhOemFXOXVMWGRsWW1odmIyc3RjM1pqTG1Sd0xYTjVjM1JsYllJcGJIaGoKWm5NdFlXUnRhWE56YVc5dUxYZGxZbWh2YjJzdGMzWmpMbVJ3TFhONWMzUmxiUzV6ZG1Nd0RRWUpLb1pJaHZjTgpBUUVMQlFBRGdnRUJBRGsrSXZnbm5nZkprdE80QVpsZWdET2F1c0U1OEZkNHI3U3cyYzhqcWY5dGpvM1BHUy94CllTdU1xbWMyRGR4V0trRlcydWpTOTZnTERGemFjY3NFQkg3OVVvaWZxTEM2My9oY3VzUSttQkhYK3ZpVXhGamIKT05qUFJCTEZJRXFXVjZJamhPTVdaUGxaVjNTdVJuVGN6U2N4N2F3RFlxWDZmVm9QRzB6djFIVXllcDFCak83UgpETFdXVVlNR3lHNFVPK3VGby9iVVVURVp3c0hYekxydmROSkFoQVFhRDNPeU1ic25UR0F3SmdGYkp6MDlNRURuClY4Uk1Uald3cTBBbmZEeEJESVBJOHBCSk9YV2Y3TmUxcFFSd3FqTFVyM3VnK0VUWDhpNVFEQUVnNzNDcUR1UkQKUHE1a2xGTXFZYnlEYTNsd21pUldHbmc5VTZuSmx0NTVmVzg9Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K");
        data.put("key.pem", "LS0tLS1CRUdJTiBSU0EgUFJJVkFURSBLRVktLS0tLQpNSUlFcEFJQkFBS0NBUUVBcjVpQTJwMHpHV2ZjTWI1VWNyODRuSGQ1UFNzZzBYNDNrM1pZeEl5WWNZMGJ2c0NICm1veXZWVThScVhIUDk4OW1hajVPZUtHWEptelJUa3NoRFFFSFpQbXQzSlEzL05iajhCZWVQY1k4UittTGpwZlcKNWFMZGRiTDZBTm5yQWpXUnlQN1paRndKdnlIZmVyUFEwSkVJWDN2bzNmMXJhVERQVTh4U2RSZzBnK05RTDlXZwpNTHBpMVNsek5ya3pGVGNoQW9vWGdzTitEaDliQnVhbGFmK3VPRDV1U2xrRjdLWHRtNE9pZXN1NDdET21NUG8zCnRHejQvR2dlL2IzNWJ2UDVrZkxWTTF1VDZDeHdXSUgwbHB1QUFxMUJ0ell3dnpuMDVPRVY0NjAxN1htRDZ2VmIKekFucUxKUjA5SGU2L1FBdzVIR0tVU0wxMHEzUWdjMDRCS0NITlFJREFRQUJBb0lCQVFDb1B3MG5hbnZ0bWRBbgpjV0JrMmNlYVUvYzhucmhCUWhocUdIa1JTazArYjUvbjgzMTZuZFhaZlh0RXlhSWtwUHBTVGdUT0hMWkF3UDhECmI0VHBldFRrOFQ2ZkhQMVFLMjRYTytvSThvb0x0VUJjaml2L3R6OU9hUUNXRkRiUzFSVXNhdE5ORDhyZThjdTUKTzlXV0lZRE9UR0ZoWWxHcVpuUWdyS05OUXJYK3J4VVNLV1lJM0xxd0s1T2lJaEI0TTlQWm9EOXFZZXpybFo1MAp4dzJoZGkxSVJ3OEM2cjFMVGtmU0xzWmsyc3gwcDU0SWNZSUNCSG9MbjFjMTE0c2lVaGRnWTJraXd1SG5RVEtTCkJ1Qlh0T3pvV054RXJBMmVXUDJpWGtGZkxqMWJGalNnUHhqNmlGOFUyZ29TUnlNWStqaDJoVmRTcit6SmMrVGQKbTBoejc5QkJBb0dCQU9aV2J4VWFRck1lTVlYVDVwU3pRc3R4V0dhWk1zaHFwdERxRzlha2tYU2M4cEsyMmZUYgpZcVQ1L0xUS0JHMUdzVmNKaEsvb1BWaldUdGp3WlREOFB0QzBuekNTTkpDZktBSS8xUFhkdlcvVzVZampNaHQ4ClFUWHRkdUZXZVc3MUFJU3huWWFZZ2YwVzgyOGkrVjRDYkpWQmRLOWw5d0V1YnF6ak40OVhQTGFaQW9HQkFNTW8Kdi9yWEx4aW9HRnJxUmJhTWlCUDlWL254SnAzOG9QY1JhV2NRR2prZnExc3FISXVacUtNWUFWaFBJanhrcmRqTApxdEFNaW5ETTJmY2cwSXhFUGtHWnk0Y3ZuT0VVZ3ZGWE16NUhCV1RUTzRhVVJQb0xDUUVndEpOamtDd1NCUkhTCkZBdnNBb1NLcDFpZ0hreXJ5bXRHUGFVSDlRYlRwaThCQmVDVTllTDlBb0dBSVdIQmM1Tjh1eXpUREl3clRMMjIKSG5uc3orSEFCRi9Ba1dKOGFsMHJYM1VuaXEvSllyMHd6S3dXUGJWUmN1emQ0cmxVYTVJQzRnOGRHaitzeXE2awpBK2RKY3VhZGk2QWxVajRpQlFmLzNtZU1tSXdreE9yN0lHK3IzUGlGWE5sUkcrb0o1R25SM1BZb2phREo5eC91CjBLck4yR1cwcUkwc0tRNDlEUjhkZzNFQ2dZQUN1OUZrbkJzR3gyUC84Z1FqK0hmM2Y3YjV3MHo5TFlxN3F6ekUKdXM4RE9kUldwSkpkeWpzNGZaK1hNeFNSci85WGFCTHVjeDZGVWZsRG91d0k2dlBNUzNFMFpaQlcvc2lPeE4vcwo0V1hMOGNHRnN4SDhScWRMb3Ira3lPSStHOHFDWTJUd2ZjL1ROM1g4b2RHSzFXVXlkM3Rwa21EMlJZU3daOTFECk5oTHpxUUtCZ1FDYm9SY3JyWGRYV2k4ZEU4aytoRlpsU1R6MHZYZnRETkdxZ0gxa01yQVUrVTY4N3NnZkRvMjEKSTlHN29aeFl2NDcxK2lZZk9tdzdoTzRaODB0U3lIYUNTdzd3WjduSnFzNlhra1ZkSTkyVXVaQ3MycGxaR01nUgpWUC9jTmNycWp6MzVrSm9OalhqT2VHZU9lb1VONE1qc1A4dXc3N29HWjd1QmZjUWNaZHBibnc9PQotLS0tLUVORCBSU0EgUFJJVkFURSBLRVktLS0tLQo=");

        Secret build = new SecretBuilder()
                .withNewMetadata()
                .withName(LXCFS_SECRET_NAME)
                .withNamespace(LXCFS_NAMESPACE)
                .withLabels(label)
                .endMetadata()
                .withData(data)
                .build();
        client.secrets().inNamespace(LXCFS_NAMESPACE).createOrReplace(build);
    }

    /**
     * CertificateSigningRequest
     */
    public void createLxcfsCsr() {
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        CertificateSigningRequest csr = client.certificateSigningRequests().withName(LXCFS_CSR_NAME).get();
        if (csr != null) {
            return;
        }

        Map<String, String> label = Maps.newHashMap();
        label.put("app.kubernetes.io/component", "lxcfs-csr");
        label.put("app.kubernetes.io/instance", "lxcfs-csr");
        label.put("app.kubernetes.io/name", "lxcfs-csr");

        CertificateSigningRequest build = new CertificateSigningRequestBuilder()
                .withNewMetadata()
                .withLabels(label)
                .withName(LXCFS_CSR_NAME)
                .endMetadata()
                .withNewSpec()
                .withGroups(Lists.newArrayList("system:masters", "system:authenticated"))
                .withRequest(LXCFS_CSR_REQUEST)
                .withSignerName(LXCFS_CSR_SIGNER)
                .withUsages(Lists.newArrayList("digital signature", "key encipherment", "server auth"))
                .withUsername("kubernetes-admin")
                .endSpec()
                .build();
        client.certificateSigningRequests().createOrReplace(build);

    }


    /**
     * daemonset
     *
     * @param registryUrl
     */
    public void createLxcfsDaemonset(String registryUrl) {
        DefaultKubernetesClient client = k8sClientHolder.getClient();
        DaemonSet daemonSet = client.apps().daemonSets().inNamespace(LXCFS_NAMESPACE).withName(LXCFS_DAEMONSET_NAME).get();
        if (daemonSet != null) {
            return;
        }
        Map<String, String> label = Maps.newHashMap();
        label.put("app.kubernetes.io/component", "lxcfs");
        label.put("app.kubernetes.io/instance", "lxcfs");
        label.put("app.kubernetes.io/name", "lxcfs");
        label.put("dolphin/service-level", "L1");
        Map<String, String> selector = Maps.newHashMap();
        selector.put("app.kubernetes.io/instance", "lxcfs");

        Map<String, Quantity> requests = Maps.newHashMap();
        requests.put("cpu", new Quantity("0", null));
        requests.put("memory", new Quantity("0", "Mi"));
        Map<String, Quantity> limits = Maps.newHashMap();
        limits.put("cpu", new Quantity("1", null));
        limits.put("memory", new Quantity("1024", "Mi"));

        List<VolumeMount> volumeMounts = Lists.newArrayList();
        volumeMounts.add(new VolumeMountBuilder().withName("cgroup").withMountPath("/sys/fs/cgroup").build());
        volumeMounts.add(new VolumeMountBuilder().withName("lxcfs").withMountPath("/var/lib/lxcfs").withMountPropagation("Bidirectional").build());
        volumeMounts.add(new VolumeMountBuilder().withName("usr-local").withMountPath("/usr/local").build());
        volumeMounts.add(new VolumeMountBuilder().withName("usr-lib64").withMountPath("/usr/lib64").build());
        Container container = new ContainerBuilder()
                .withImagePullPolicy(K8sConstants.IMAGE_PULL_POLICY_RESENT)
                .withImage(registryUrl.concat("/denverdino/lxcfs:3.1.2"))
                .withName(LXCFS_DAEMONSET_NAME)
                .withNewResources()
                .withRequests(requests)
                .withLimits(limits)
                .endResources()
                .withSecurityContext(new SecurityContextBuilder().withPrivileged(true).build())
                .withTerminationMessagePath("/dev/termination-log")
                .withTerminationMessagePolicy("File")
                .withVolumeMounts(volumeMounts)
                .build();

        DaemonSetUpdateStrategy rollingUpdate = new DaemonSetUpdateStrategyBuilder()
                .withType("RollingUpdate")
                .withNewRollingUpdate()
                .withMaxSurge(new IntOrStringBuilder().withStrVal("25%").build())
                .withMaxUnavailable(new IntOrStringBuilder().withStrVal("25%").build())
                .endRollingUpdate()
                .build();


        List<Volume> volumes = Lists.newArrayList();
        volumes.add(new VolumeBuilder().withName("cgroup").withHostPath(new HostPathVolumeSourceBuilder().withType("").withPath("/sys/fs/cgroup").build()).build());
        volumes.add(new VolumeBuilder().withName("lxcfs").withHostPath(new HostPathVolumeSourceBuilder().withType("").withPath("/var/lib/lxcfs").build()).build());
        volumes.add(new VolumeBuilder().withName("usr-local").withHostPath(new HostPathVolumeSourceBuilder().withType("").withPath("/usr/local").build()).build());
        volumes.add(new VolumeBuilder().withName("usr-lib64").withHostPath(new HostPathVolumeSourceBuilder().withType("").withPath("/usr/lib64").build()).build());
        DaemonSet build = new DaemonSetBuilder()
                .withNewMetadata()
                .withName(LXCFS_DAEMONSET_NAME).withNamespace(LXCFS_NAMESPACE).withLabels(label)
                .endMetadata()
                .withNewSpec()
                .withUpdateStrategy(rollingUpdate)
                .withNewSelector().withMatchLabels(selector).endSelector()
                .withNewTemplate()
                .withMetadata(new ObjectMetaBuilder().withLabels(selector).build())
                .withNewSpec()
                .withContainers(container)
                .withDnsPolicy(K8sConstants.CLUSTER_FIRST)
                .withHostPID(true)
                .withRestartPolicy(K8sConstants.RESTART_POLICY_ALWAYS)
                .withTolerations(new TolerationBuilder().withOperator("Exists").build())
                .withPriorityClassName("dolphin-level-1")
                .withVolumes(volumes)
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
        client.apps().daemonSets().inNamespace(LXCFS_NAMESPACE).createOrReplace(build);
    }
}
