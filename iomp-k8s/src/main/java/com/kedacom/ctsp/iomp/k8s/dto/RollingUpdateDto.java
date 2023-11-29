package com.kedacom.ctsp.iomp.k8s.dto;

import lombok.Data;

/**
 * @author tangjixing
 * @date 2020/4/20
 */
@Data
public class RollingUpdateDto {

    /**
     * 升级策略类型  滚动升级
     */
    public static final String STRATEGY_ROLLING_UPDATE = "RollingUpdate";

    /**
     * pod最大数量
     * 可以为整数或者百分比，默认为desired Pods数的25%. Scale Up新的ReplicaSet时，按照比例计算出允许的MaxSurge，计算时向上取整(比如3.4，取4)
     */
    private String maxSurge;

    /**
     * 不可用的pod最大数量
     * 可以为整数或者百分比，默认为desired Pods数的25%. Scale Down旧的ReplicaSet时，按照比例计算出允许的maxUnavailable，计算时向下取整(比如3.6，取3)
     */
    private String maxUnavailable;
}
