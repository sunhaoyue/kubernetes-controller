package com.kedacom.ctsp.iomp.k8s.dto;

import com.alibaba.fastjson.JSONArray;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.DaemonSetUpdateStrategy;
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategy;
import io.fabric8.kubernetes.api.model.apps.StatefulSetUpdateStrategy;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author tangjixing
 * @date 2020/4/20
 */
@Data
public class DeploymentEditDto {

    private String clusterId;

    private String clientId;


    private String name;

    private String namespace;

    /**
     * 实例数
     */
    private Integer replicas;


    /**
     * 带单位。  xxx m
     */
    private String limitCpu;

    /**
     * 带单位的字符串
     */
    private String limitMemory;

    private Long requestGpu;

    /**
     * 带单位的字符串
     */
    private String requestMemory;

    private String projectImage;

    private String baseImage;

    private Map<String, String> releaseInfo;


    /**
     * 升级模式，
     * 是否是滚动升级，如果是，则滚动升级的相关参数
     */
    private DeploymentStrategy strategy;

    private StatefulSetUpdateStrategy statefulSetStrategy;

    private DaemonSetUpdateStrategy daemonSetStrategy;

    private Affinity affinity;

    private List<Toleration> tolerations;


    /**
     * 主容器的环境变量，不考虑其他容器的编辑
     * <p>
     * 这里考虑要进行全量更新，因为编辑的时候可能会出现 删除的情况
     */
    private Map<String, EnvVar> env;


    /**
     * 挂载的目录，对pvc进行判断， 根据name进行处理
     * <p>
     * 要考虑删除的情况
     */
    private List<VolumeDto> volumes;

    /**
     * 容器端口，hostPort，
     */
    private List<ContainerPort> ports;

    /**
     * 保活探针
     */
    private Probe livenessProbe;

    /**
     * 就绪探针
     */
    private Probe readinessProbe;

    /**
     * bgp字符串  '["10.65.2.100","10.65.2.101"]'
     */
    private String bgp;

    private String ipv4pools;

    private String logStatus;

    private Container newLogContainer;

    private Container newApmContainer;

    private Container newPromContainer;

    private String apmStatus;

    private Map<String, String> labels;

    private Map<String, String> annotations;

    private String serviceAccount;

    /**
     * 开启自定义指标采集 1 开启 ，0 不开启
     */
    private Integer enableCustomMetric;

    /**
     * 平台字段： 用于控制是否为部分更新
     */
    private Boolean isPatch = false;

    /**
     * pvc 模板
     */
    private JSONArray newLocalPvcTemplates;

    private Boolean isHostNetWork = false;

    private String priorityClassName;

}
