package com.kedacom.ctsp.iomp.k8s.vo;

import io.fabric8.kubernetes.api.model.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 部署信息(非映射数据库表，是部署时使用的部署信息)
 *
 * @author hefei
 * @create 2018-01-13 上午9:13
 **/
@Data
public class K8sJobDeployVo {

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
     * 指定节点ip
     */
    private String nodeName;
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
    private String command;
    /**
     * 运行参数
     */
    private String[] args;
    /**
     * 显卡数
     */
    private Integer gpuSize;
    /**
     * 挂载原型
     */
    private VolumeMount[] volumeMounts;
    /**
     * 挂载目录
     */
    private Volume[] volumes;
    /**
     * 重启机制 Never或OnFailure
     */
    private String restartPolicy;






    /**
     * 实例数
     */
    @Deprecated
    private Integer replicas;

    /**
     * 添加环境
     */
    @Deprecated
    private EnvVar[] env;
}
