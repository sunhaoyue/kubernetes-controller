package com.kedacom.ctsp.iomp.k8s.vo;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 *  k8s vo
 *
 * @author hef
 **/
@Data
public class K8sBuildVo {

    /**
     * 命名空间
     */
    private String nameSpace;
    /**
     * API版本
     */
    private String apiVersion;
    /**
     * 类型
     */
    private String kind;

    /**
     * 复制几个pods
     */
    private Integer replicas;

    /**
     * 标签
     */
    private Map<String, String> labels;

    /**
     * 任务名
     */
    private String name;
    /**
     * 镜像名
     */
    private String image;
    /**
     * 镜像拉取策略
     */
    private String imagePullPolicy;
    /**
     * 命令
     */
    private List<String> command;
    /**
     * 运行参数
     */
    private List<String> args;

    /**
     * 显卡数
     */
    private int gpuSize;

    /**
     * 挂载原型
     */
    private List<VolumeMount> volumeMounts;

    /**
     * 挂载目录
     */
    private List<Volume> volumes;

    /**
     * 重启机制 Never、OnFailure或always
     */
    private String restartPolicy;

    /**
     * 指定节点ip
     */
    private String nodeName;

}
