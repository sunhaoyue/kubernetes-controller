package com.kedacom.ctsp.iomp.k8s.convert;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.kedacom.ctsp.iomp.k8s.constant.K8sConstants;
import com.kedacom.ctsp.iomp.k8s.dto.DeploymentEditDto;
import com.kedacom.ctsp.iomp.k8s.dto.VolumeDto;
import com.kedacom.ctsp.iomp.k8s.enmu.CustomMetricEnum;
import com.kedacom.ctsp.lang.exception.CommonException;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.DaemonSetUpdateStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.kedacom.ctsp.iomp.k8s.constant.K8sMountConstant.*;

/**
 * @author tangjixing
 * @date 2020/4/20
 */
@Slf4j
public class DaemonSetConvert implements K8sConvert<DaemonSet, DeploymentEditDto> {


    @Override
    public DeploymentEditDto convert(DaemonSet from) {
        DeploymentEditDto result = new DeploymentEditDto();
        Container mainContainer = getMainContainer(from);

        Map<String, Quantity> limits = mainContainer.getResources().getLimits();
        Map<String, Quantity> requests = mainContainer.getResources().getRequests();

        /**
         * 部署包升级的地方有处理
         */
        result.setLimitCpu(limits.get("cpu").getAmount());
        result.setLimitMemory(limits.get("memory").getAmount());
        result.setRequestMemory(requests.get("memory").getAmount());

        result.setPorts(mainContainer.getPorts());
        result.setLivenessProbe(mainContainer.getLivenessProbe());
        result.setReadinessProbe(mainContainer.getReadinessProbe());
        result.setEnv(convertEnv(mainContainer.getEnv()));
        List<Volume> volumes = from.getSpec().getTemplate().getSpec().getVolumes();
        result.setVolumes(volumeDto(mainContainer.getVolumeMounts(), volumes));
        return result;
    }

    private Map<String, EnvVar> convertEnv(List<EnvVar> env) {
        if (CollectionUtils.isEmpty(env)) {
            return new HashMap<>();
        }
        Map<String, EnvVar> result = new HashMap<>();
        for (EnvVar envVar : env) {
            result.put(envVar.getName(), envVar);
        }
        return result;
    }


    private int parseCpu(String cpu) {
        if (StringUtils.isNumeric(cpu)) {
            return Integer.parseInt(cpu) * 1000;
        }
        return 0;
    }

    private int parseMemory(String memoryUsable) {
        try {
            if (StringUtils.endsWith(memoryUsable, "Gi")) {
                return new Double(Double.parseDouble(StringUtils.removeEndIgnoreCase(memoryUsable, "Gi")) * 1024).intValue();
            }
            if (StringUtils.endsWith(memoryUsable, "Mi")) {
                return new Double(Double.parseDouble(StringUtils.removeEndIgnoreCase(memoryUsable, "Mi"))).intValue();
            }
            return 0;
        } catch (Exception e) {
            log.error("不是合法的内存数据", e);
            throw new CommonException("iomp.k8s.data.illegal" , memoryUsable);
        }

    }


    private List<VolumeDto> volumeDto(List<VolumeMount> volumeMounts, List<Volume> volumes) {
        if (CollectionUtils.isEmpty(volumeMounts)) {
            return new ArrayList<>();
        }
        Map<String, Volume> volumeMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(volumes)) {
            volumeMap = volumes.stream().collect(Collectors.toMap(v -> v.getName(), v -> v, (val1, val2) -> val2));
        }

        List<VolumeDto> result = new ArrayList<>();
        for (VolumeMount volumeMount : volumeMounts) {
            Volume volume = volumeMap.get(volumeMount.getName());
            if (volume == null) {
                continue;
            }
            if (volume.getPersistentVolumeClaim() != null) {
                VolumeDto dto = new VolumeDto();
                dto.setName(volumeMount.getName());
                dto.setMountPath(volumeMount.getMountPath());
                dto.setClaimName(volume.getPersistentVolumeClaim().getClaimName());
                dto.setType(VolumeDto.TYPE_PVC);
                result.add(dto);
                continue;
            }

            if (volume.getSecret() != null) {
                VolumeDto dto = new VolumeDto();
                dto.setName(volumeMount.getName());
                dto.setMountPath(volumeMount.getMountPath());
                dto.setSecretName(volume.getSecret().getSecretName());
                dto.setType(VolumeDto.TYPE_SECRET);
                result.add(dto);
                continue;
            }
        }
        return result;
    }


    /**
     * 获取主容器
     *
     * @param from
     * @return
     */
    private Container getMainContainer(DaemonSet from) {
        List<Container> containers = from.getSpec().getTemplate().getSpec().getContainers();
        if (CollectionUtils.isEmpty(containers)) {
            throw new CommonException("iomp.k8s.container.notfound" , from.getMetadata().getName());
        }

        for (Container container : containers) {
            if (StringUtils.indexOf(container.getName(), from.getMetadata().getName()) >= 0) {
                return container;
            }
        }
        throw new CommonException("iomp.k8s.container.notfound" ,from.getMetadata().getName());
    }

    private Container geInitContainer(DaemonSet from) {
        List<Container> containers = from.getSpec().getTemplate().getSpec().getInitContainers();
        if (CollectionUtils.isEmpty(containers)) {
            return null;
        }
        return containers.get(0);
    }


    /**
     * daemonset 没有replicas字段
     *
     * @param source
     * @param target
     * @return
     */
    @Override
    public DaemonSet merge(DeploymentEditDto source, DaemonSet target) {
        if (source == null || target == null) {
            return target;
        }
        log.info("last daemonset:" + JSONObject.toJSONString(target));
        DaemonSet newDeployment = new DaemonSetBuild(target)
                .setIsPatch(source.getIsPatch())
                .setAffinity(source.getAffinity())
                .setRollingUpdate(source.getDaemonSetStrategy())
                .setLimitCpu(source.getLimitCpu())
                .setLimitGpu(source.getRequestGpu())
                .setLimitMemory(source.getLimitMemory())
                .setRequestMemory(source.getRequestMemory())
                .setLivenessProbe(source.getLivenessProbe())
                .setReadinessProbe(source.getReadinessProbe())
                .setPorts(source.getPorts())
                .setHostNetWork(source.getIsHostNetWork())
                .setEnv(source.getEnv())
                .setBgp(source.getBgp())
                .setIpv4pools(source.getIpv4pools())
                .setImage(source.getProjectImage())
                .setBaseImage(source.getBaseImage())
                .setLabels(source.getLabels())
                .setAnnotations(source.getAnnotations())
                .setServiceAccount(source.getServiceAccount())
                .setVolumeMounts(source.getVolumes())
                .setLogContainer(source.getLogStatus(), source.getNewLogContainer(), source.getName())
                .setApmContainer(source.getApmStatus(), source.getNewApmContainer())
                .setPromContainer(source.getEnableCustomMetric(), source.getNewPromContainer())
                .setTolerations(source.getTolerations())
                .setPriorityClass(source.getPriorityClassName())
                .build();
        log.info("new deployment:" + JSONObject.toJSONString(target));
        return newDeployment;
    }


    class DaemonSetBuild {

        private final DaemonSet deployment;

        private Boolean isPatch;

        private final String label_version = "app.kubernetes.io/version";

        private final String label_managedBy = "app.kubernetes.io/managed-by";

        private DaemonSetBuild(DaemonSet deployment) {
            this.deployment = deployment;
        }

        public DaemonSet build() {
            return deployment;
        }

        public DaemonSetBuild setIsPatch(Boolean isPatch) {
            if (null == isPatch) {
                isPatch = Boolean.FALSE;
            }
            this.isPatch = isPatch;
            return this;
        }

        public DaemonSetBuild setLimitGpu(Long gpu) {
            if (null == gpu && this.isPatch) {
                return this;
            }
            Map<String, Quantity> limits = getMainContainer(deployment).getResources().getLimits();
            String nvidiaGpu = "nvidia.com/gpu";
            if (gpu == null) {
                limits.remove(nvidiaGpu);
            } else {
                limits.put(nvidiaGpu, new Quantity(String.valueOf(gpu)));
            }
            return this;
        }


        public DaemonSetBuild setRollingUpdate(DaemonSetUpdateStrategy strategy) {
            if (null == strategy && this.isPatch) {
                return this;
            }
            deployment.getSpec().setUpdateStrategy(strategy);
            return this;
        }

        public DaemonSetBuild setLimitCpu(String cpu) {
            if (null == cpu && this.isPatch) {
                return this;
            }
            Map<String, Quantity> limits = getMainContainer(deployment).getResources().getLimits();
            limits.put("cpu", new Quantity(cpu));
            return this;
        }

        public DaemonSetBuild setLimitMemory(String memory) {
            if (null == memory && this.isPatch) {
                return this;
            }
            Map<String, Quantity> limits = getMainContainer(deployment).getResources().getLimits();
            limits.put("memory", new Quantity(memory));
            return this;
        }

        public DaemonSetBuild setRequestMemory(String memory) {
            if (null == memory && this.isPatch) {
                return this;
            }
            Map<String, Quantity> requests = getMainContainer(deployment).getResources().getRequests();
            requests.put("memory", new Quantity(memory));
            return this;
        }

        public DaemonSetBuild setPorts(List<ContainerPort> ports) {
            if (null == ports && this.isPatch) {
                return this;
            }
            getMainContainer(deployment).setPorts(ports);
            return this;
        }

        public DaemonSetBuild setLivenessProbe(Probe probe) {
            if (null == probe && this.isPatch) {
                return this;
            }
            getMainContainer(deployment).setLivenessProbe(probe);
            return this;
        }

        public DaemonSetBuild setReadinessProbe(Probe probe) {
            if (null == probe && this.isPatch) {
                return this;
            }
            getMainContainer(deployment).setReadinessProbe(probe);
            return this;
        }

        public DaemonSetBuild setEnv(Map<String, EnvVar> env) {
            if (MapUtils.isEmpty(env)) {
                return this;
            }
            Container mainContainer = getMainContainer(deployment);
            List<EnvVar> originalEnv = mainContainer.getEnv();
            for (EnvVar var : originalEnv) {
                if (!StringUtils.equals("TZ", var.getName())
                        && env.get(var.getName()) == null) {
                    env.put(var.getName(), var);
                }
            }
            mainContainer.setEnv(new ArrayList<>(env.values()));
            return this;
        }

        public DaemonSetBuild setImage(String projectImage) {
            if (StringUtils.isBlank(projectImage)) {
                return this;
            }
            // daemon set 不考虑init
            /*Container container = geInitContainer(deployment);
            if (container != null) {
                container.setImage(projectImage);
            }*/
            return this;
        }

        public DaemonSetBuild setBaseImage(String baseImage) {
            if (StringUtils.isBlank(baseImage)) {
                return this;
            }
            getMainContainer(deployment).setImage(baseImage);
            return this;
        }

        public DaemonSetBuild setBgp(String bgp) {
            if (null == bgp && this.isPatch) {
                return this;
            }
            String key = K8sConstants.IP_ADDRS_NO_IPAM;
            Map<String, String> annotations = deployment.getSpec().getTemplate().getMetadata().getAnnotations();
            if (StringUtils.isBlank(bgp)) {
                if (annotations != null) {
                    annotations.remove(key);
                    deployment.getSpec().getTemplate().getMetadata().setAnnotations(annotations);
                }
                return this;
            }

            if (annotations == null) {
                annotations = new HashMap<>();
            }
            annotations.put(key, bgp);
            deployment.getSpec().getTemplate().getMetadata().setAnnotations(annotations);
            return this;
        }

        public DaemonSetBuild setIpv4pools(String ipv4pools) {
            if (null == ipv4pools && this.isPatch) {
                return this;
            }
            String key = K8sConstants.IPV4_POOLS;
            Map<String, String> annotations = deployment.getSpec().getTemplate().getMetadata().getAnnotations();
            if (StringUtils.isBlank(ipv4pools)) {
                if (annotations != null) {
                    annotations.remove(key);
                    deployment.getSpec().getTemplate().getMetadata().setAnnotations(annotations);
                }
                return this;
            }

            if (annotations == null) {
                annotations = new HashMap<>();
            }
            annotations.put(key, ipv4pools);
            deployment.getSpec().getTemplate().getMetadata().setAnnotations(annotations);
            return this;
        }

        public DaemonSetBuild setAnnotations(Map<String, String> ann) {
            if (null == ann && this.isPatch) {
                return this;
            }
            Map<String, String> annotations = deployment.getMetadata().getAnnotations();
            if (annotations == null) {
                annotations = new HashMap<>();
            }
            String createCoder = annotations.get("dolphin/creator");
            annotations.putAll(ann);
            annotations.put("dolphin/creator", StringUtils.isNotEmpty(createCoder) ? createCoder : "");
            deployment.getMetadata().setAnnotations(annotations);
            return this;
        }

        public DaemonSetBuild removePvc() {
            List<Volume> volumes = deployment.getSpec().getTemplate().getSpec().getVolumes();
            if (CollectionUtils.isEmpty(volumes)) {
                return this;
            }

            List<VolumeMount> volumeMounts = getMainContainer(deployment).getVolumeMounts();
            Map<String, VolumeMount> volumeMountMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(volumes)) {
                volumeMountMap = volumeMounts.stream().collect(Collectors.toMap(v -> v.getName(), v -> v, (val1, val2) -> val2));
            }

            List<Volume> lastVolumes = new ArrayList<>();
            for (Volume volume : volumes) {
                if (volume.getPersistentVolumeClaim() != null
                        && StringUtils.isNotBlank(volume.getPersistentVolumeClaim().getClaimName())) {
                    volumeMountMap.remove(volume.getName());
                    continue;
                }
                lastVolumes.add(volume);
            }

            deployment.getSpec().getTemplate().getSpec().setVolumes(lastVolumes);
            getMainContainer(deployment).setVolumeMounts(new ArrayList<>(volumeMountMap.values()));
            return this;
        }

        public DaemonSetBuild setVolumeMounts(List<VolumeDto> volumeDtos) {
            if (null == volumeDtos && this.isPatch) {
                return this;
            }

            // 分两步， 移除挂载的pvc， 然后合并加入的存储
            /**
             * 移除业务的配置
             */
            removePvc();
            //先移除ca，kubectl的挂载，再赋值新的
            removeCaAndKubectlAndTimezoneMount();

            if (CollectionUtils.isEmpty(volumeDtos)) {
                List<Volume> volumes = deployment.getSpec().getTemplate().getSpec().getVolumes();
                if (CollectionUtils.isEmpty(volumes)) {
                    volumes = Lists.newArrayList();
                }
                for (Volume originalVolume : volumes) {
                    if (StringUtils.equals(originalVolume.getName(), "rootlog")) {
                        return this;
                    }
                }
                Volume build = new VolumeBuilder()
                        .withName("rootlog")
                        .withNewHostPath()
                        .withPath("/data/logs/" + deployment.getMetadata().getNamespace() + "/" + deployment.getMetadata().getName())
                        .endHostPath().build();
                volumes.add(build);
                deployment.getSpec().getTemplate().getSpec().setVolumes(volumes);
                return this;
            }

            Map<String, VolumeMount> volumeMountMap = new HashMap<>();
            Map<String, Volume> volumeMap = new HashMap<>();
            List<Volume> volumes = deployment.getSpec().getTemplate().getSpec().getVolumes();
            List<VolumeMount> volumeMounts = getMainContainer(deployment).getVolumeMounts();
            if (CollectionUtils.isNotEmpty(volumes)) {
                volumeMap = volumes.stream().collect(Collectors.toMap(v -> v.getName(), v -> v, (val1, val2) -> val2));
            }

            if (CollectionUtils.isNotEmpty(volumeMounts)) {
                volumeMountMap = volumeMounts.stream().collect(Collectors.toMap(v -> v.getName(), v -> v, (val1, val2) -> val2));
            }

            for (VolumeDto volumeDto : volumeDtos) {

                Volume volume = new Volume();
                volume.setName(volumeDto.getName());
                if (StringUtils.isNotEmpty(volumeDto.getClaimName())) {
                    volume.setPersistentVolumeClaim(new PersistentVolumeClaimVolumeSource(volumeDto.getClaimName(), false));
                } else if (StringUtils.isNotEmpty(volumeDto.getSecretName())) {
                    volume.setSecret(new SecretVolumeSource(420, new ArrayList(), null, volumeDto.getSecretName()));
                } else if (StringUtils.equals(volumeDto.getType(), VolumeDto.TYPE_HOSTPATH)) {
                    String hostPath = StringUtils.isNotEmpty(volumeDto.getMountHardcorePath()) ? volumeDto.getMountHardcorePath() : volumeDto.getMountPath();
                    volume.setHostPath(new HostPathVolumeSourceBuilder().withPath(hostPath).withType("").build());
                }
                volumeMap.put(volumeDto.getName(), volume);

                VolumeMount volumeMount = new VolumeMount();
                volumeMount.setName(volumeDto.getName());
                volumeMount.setMountPath(volumeDto.getMountPath());
                if (StringUtils.equals(volumeDto.getName(), TIMEZONE_VOLUME)) {
                    volumeMount.setMountPath("/etc/localtime");
                }
                volumeMountMap.put(volumeDto.getName(), volumeMount);
            }

            if (volumeMap.get("rootlog") == null) {
                Volume build = new VolumeBuilder()
                        .withName("rootlog")
                        .withNewHostPath()
                        .withPath("/data/logs/" + deployment.getMetadata().getNamespace() + "/" + deployment.getMetadata().getName())
                        .endHostPath().build();
                volumeMap.put("rootlog", build);
            }

            deployment.getSpec().getTemplate().getSpec().setVolumes(new ArrayList<>(volumeMap.values()));
            getMainContainer(deployment).setVolumeMounts(new ArrayList<>(volumeMountMap.values()));
            return this;
        }

        private DaemonSetBuild removeCaAndKubectlAndTimezoneMount() {
            List<Volume> volumes = deployment.getSpec().getTemplate().getSpec().getVolumes();
            if (CollectionUtils.isEmpty(volumes)) {
                return this;
            }

            List<VolumeMount> volumeMounts = getMainContainer(deployment).getVolumeMounts();
            Map<String, VolumeMount> volumeMountMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(volumes)) {
                volumeMountMap = volumeMounts.stream().collect(Collectors.toMap(v -> v.getName(), v -> v, (val1, val2) -> val2));
            }

            List<Volume> lastVolumes = new ArrayList<>();
            for (Volume volume : volumes) {
                if (StringUtils.equals(volume.getName(), CLUSTER_CA_VOLUME)
                        || StringUtils.equals(volume.getName(), CLUSTER_KUBECTL_VOLUME)
                        || StringUtils.equals(volume.getName(), TIMEZONE_VOLUME)) {
                    volumeMountMap.remove(volume.getName());
                    continue;
                }
                lastVolumes.add(volume);
            }

            deployment.getSpec().getTemplate().getSpec().setVolumes(lastVolumes);
            getMainContainer(deployment).setVolumeMounts(new ArrayList<>(volumeMountMap.values()));
            return this;
        }


        public DaemonSetBuild setAffinity(Affinity affinity) {
            if (null == affinity && this.isPatch) {
                return this;
            }
            deployment.getSpec().getTemplate().getSpec().setAffinity(affinity);
            return this;
        }

        public DaemonSetBuild setLogContainer(String logStatus, Container logNewContainer, String clientId) {
            if (null == logStatus && this.isPatch) {
                return this;
            }
            List<Container> containers = deployment.getSpec().getTemplate().getSpec().getContainers();
            Container logContainer = null;
            for (Container container : containers) {
                if (StringUtils.contains(container.getName(), "fluent-bit")
                        || StringUtils.contains(container.getName(), "clog-agent")) {
                    logContainer = container;
                }
            }

            if ("enabled".equals(logStatus)) {
                if (logContainer != null) {
                    // 开启日志，之前就有，
                    return this;
                }
                logContainer = logNewContainer;
                if (logContainer != null) {
                    containers.add(logContainer);
                    deployment.getSpec().getTemplate().getSpec().setContainers(containers);
                    handleClogVolume(clientId);
                }
                return this;
            }

            if (logContainer == null) {
                // 不开启日志容器，之前就没有;
                return this;
            }
            containers.remove(logContainer);
            deployment.getSpec().getTemplate().getSpec().setContainers(containers);
            return this;
        }


        public DaemonSetBuild setApmContainer(String apmStatus, Container newApmContainer) {
            if (null == apmStatus && this.isPatch) {
                return this;
            }
            List<Container> initContainers = deployment.getSpec().getTemplate().getSpec().getInitContainers();
            Container apmContainer = null;
            for (Container container : initContainers) {
                if (StringUtils.contains(container.getName(), "apm-agent")) {
                    apmContainer = container;
                }
            }
            if ("enabled".equals(apmStatus)) {
                if (apmContainer != null) {
                    // 开启日志，之前就有，
                    handleJavaOptsEnv();
                    handleContainerVolumeMount();
                    return this;
                }
                apmContainer = newApmContainer;
                if (apmContainer != null) {
                    initContainers.add(apmContainer);
                    deployment.getSpec().getTemplate().getSpec().setInitContainers(initContainers);
                    handleApmVolume();
                    handleJavaOptsEnv();
                    handleContainerVolumeMount();
                }
                return this;
            }
            if (apmContainer == null) {
                // 不开启日志容器，之前就没有;
                return this;
            }
            initContainers.remove(apmContainer);
            deployment.getSpec().getTemplate().getSpec().setInitContainers(initContainers);
            return this;
        }

        private void handleContainerVolumeMount() {
            //给容器添加apm挂载
            Container mainContainer = getMainContainer(deployment);
            List<VolumeMount> volumeMounts = mainContainer.getVolumeMounts();
            if (CollectionUtils.isEmpty(volumeMounts)) {
                volumeMounts = Lists.newArrayList();
            }
            boolean addMount = true;
            for (VolumeMount mount : volumeMounts) {
                if (StringUtils.equals(mount.getName(), "apm-agent")) {
                    addMount = false;
                }
            }
            if (addMount) {
                volumeMounts.add(new VolumeMountBuilder().withName("apm-agent").withMountPath("/apm-agent").build());
            }
        }


        public void handleClogVolume(String clientId) {
            List<Volume> volumes = deployment.getSpec().getTemplate().getSpec().getVolumes();
            boolean logVolumeExsit = false;
            for (Volume volume : volumes) {
                if (StringUtils.equals(volume.getName(), "clog-config")) {
                    logVolumeExsit = true;
                }
            }
            if (!logVolumeExsit) {
                volumes.add(new VolumeBuilder().withName("clog-config").withNewConfigMap().withName("clog-" + clientId).endConfigMap().build());
            }
        }

        public void handleApmVolume() {
            List<Volume> volumes = deployment.getSpec().getTemplate().getSpec().getVolumes();
            boolean apmVolumeExsit = false;
            for (Volume volume : volumes) {
                if (StringUtils.equals(volume.getName(), "apm-agent")) {
                    apmVolumeExsit = true;
                }
            }
            if (!apmVolumeExsit) {
                volumes.add(new VolumeBuilder().withName("apm-agent").withNewEmptyDir().endEmptyDir().build());
            }
        }


        public void handleJavaOptsEnv() {
            //给有java-opts的容器的天价apm的env变量值
            List<Container> containers = deployment.getSpec().getTemplate().getSpec().getContainers();
            containerCircle:
            for (Container container : containers) {
                List<EnvVar> envVars = container.getEnv();
                envCircle:
                for (EnvVar env : envVars) {
                    if (StringUtils.equals(env.getName(), "JAVA_OPTS")) {
                        if (StringUtils.contains(env.getValue(), "-javaagent:/apm-agent/skywalking-agent.jar")) {
                            break containerCircle;
                        }
                        String java_opts = env.getValue() + "  -javaagent:/apm-agent/skywalking-agent.jar";
                        env.setValue(java_opts);
                        break containerCircle;
                    }
                }
            }
        }


        public DaemonSetBuild setLabels(Map<String, String> labels) {
            if (null == labels && this.isPatch) {
                return this;
            }
            deployment.getMetadata().setLabels(labels);
            Map<String, String> matchLabels = deployment.getSpec().getSelector().getMatchLabels();
            if (matchLabels != null && matchLabels.get(label_version) != null) {
                return this;
            }
            deployment.getSpec().getTemplate().getMetadata().getLabels().put(label_version, labels.get(label_version));
            deployment.getSpec().getTemplate().getMetadata().getLabels().put(label_managedBy, labels.get(label_managedBy));
            return this;
        }

        public DaemonSetBuild setServiceAccount(String serviceAccount) {
            if (null == serviceAccount && this.isPatch) {
                return this;
            }
            deployment.getSpec().getTemplate().getSpec().setServiceAccount(serviceAccount);
            deployment.getSpec().getTemplate().getSpec().setServiceAccountName(serviceAccount);
            return this;
        }

        public DaemonSetBuild setTolerations(List<Toleration> tolerations) {
            if (CollectionUtils.isEmpty(tolerations)) {
                return this;
            }
            deployment.getSpec().getTemplate().getSpec().setTolerations(tolerations);
            return this;
        }

        public DaemonSetBuild setPromContainer(Integer enableCustomMetric, Container newPromContainer) {
            if (null == enableCustomMetric && this.isPatch) {
                return this;
            }
            List<Container> containers = deployment.getSpec().getTemplate().getSpec().getContainers();
            Container promContainer = null;
            for (Container container : containers) {
                if (StringUtils.contains(container.getName(), "apm-agent")) {
                    promContainer = container;
                }
            }
            if (enableCustomMetric != null && enableCustomMetric == CustomMetricEnum.enable.ordinal()) {
                if (promContainer != null) {
                    // 开启日志，之前就有，
                    Map<String, String> annotations = deployment.getSpec().getTemplate().getMetadata().getAnnotations();
                    if (annotations == null) {
                        annotations = Maps.newHashMap();
                    }
                    annotations.put("prometheus.io/port", "9090");
                    annotations.put("prometheus.io/scrape", "true");
                    deployment.getSpec().getTemplate().getMetadata().setAnnotations(annotations);
                    return this;
                }
                promContainer = newPromContainer;
                if (promContainer != null) {
                    containers.add(promContainer);
                    deployment.getSpec().getTemplate().getSpec().setContainers(containers);
                    Map<String, String> annotations = deployment.getSpec().getTemplate().getMetadata().getAnnotations();
                    if (annotations == null) {
                        annotations = Maps.newHashMap();
                    }
                    annotations.put("prometheus.io/port", "9090");
                    annotations.put("prometheus.io/scrape", "true");
                    deployment.getSpec().getTemplate().getMetadata().setAnnotations(annotations);
                }
                return this;
            }
            if (promContainer == null) {
                // 不开启日志容器，之前就没有;
                return this;
            }
            containers.remove(promContainer);
            deployment.getSpec().getTemplate().getSpec().setInitContainers(containers);
            return this;
        }


        public DaemonSetBuild setHostNetWork(boolean isHostNetWork) {
            if (isHostNetWork) {
                deployment.getSpec().getTemplate().getSpec().setHostNetwork(true);
                deployment.getSpec().getTemplate().getSpec().setDnsPolicy("ClusterFirstWithHostNet");
                return this;
            }
            deployment.getSpec().getTemplate().getSpec().setHostNetwork(null);
            deployment.getSpec().getTemplate().getSpec().setDnsPolicy(null);
            return this;
        }

        public DaemonSetBuild setPriorityClass(String priorityClassName) {
            if (StringUtils.isNotEmpty(priorityClassName)) {
                deployment.getSpec().getTemplate().getSpec().setPriorityClassName(priorityClassName);
            }
            return this;
        }

    }

}
