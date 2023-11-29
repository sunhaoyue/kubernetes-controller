package com.kedacom.ctsp.iomp.k8s.vo;


import lombok.Data;

@Data
public class NsDomainVo {

    /**
     * 域名证书的名称
     */
    private String secretName;

    /**
     * 原来的证书上的域名
     */
    private String originalDomain;

    /**
     * 实际的域名
     */
    private String actualDomain;

    public static NsDomainVo instance(String secretName, String originalDomain, String actualDomain) {
        return new NsDomainVo(secretName, originalDomain, actualDomain);
    }

    NsDomainVo(String secretName, String originalDomain, String actualDomain) {
        this.secretName = secretName;
        this.originalDomain = originalDomain;
        this.actualDomain = actualDomain;
    }
}
