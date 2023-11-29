package com.kedacom.ctsp.iomp.k8s.vo;

import lombok.Data;

import java.util.Map;

/**
 * @author tangjixing
 * @date 2019/11/8
 */
@Data
public class K8sSecretDto {

    private String name;

    private String namespace;

    private String certFormat;

    private String crtData;

    private String keyData;

    private String p12Data;

    private String pemData;

    Map<String, String> annotations;

    Map<String, String> labels;

    Map<String, String> data;
}
