package com.kedacom.ctsp.iomp.k8s.dto;

import lombok.Data;

/**
 * @author tangjixing
 * @date 2020/4/20
 */
@Data
public class PodPortDto {

    public static final String PROTOCOL_TCP = "TCP";

    public static final String PROTOCOL_UDP = "UDP";

    private String containerPort;

    private String protocol;
}
