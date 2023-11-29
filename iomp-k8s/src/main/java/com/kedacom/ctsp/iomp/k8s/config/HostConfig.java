package com.kedacom.ctsp.iomp.k8s.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
public class HostConfig {

    /**
     * 本机ip
     */
    private String currentIp;


}
