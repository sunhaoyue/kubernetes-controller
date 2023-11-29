package com.kedacom.ctsp.iomp.k8s.dto;

import lombok.Data;

@Data
public class RollingUpdateVo {
    /**
     * 0 滚动升级 1 覆盖升级   UpdateStrategyEnum
     */
    private Integer updateStrategy;
    /**
     * 不可用pod最大数量单位：maxUnavailableUnit  0:个，1：%   UnitEnum
     */
    private Integer maxUnavailableUnit;
    /**
     * 不可用pod最大数量
     */
    private Double maxUnavailable;
    /**
     * 超过期望的Pod数量单位：maxSurgeUnit 0:个，1：%
     */
    private Integer maxSurgeUnit;
    /**
     * 超过期望的Pod数量：maxSurge
     */
    private Double maxSurge;
}
