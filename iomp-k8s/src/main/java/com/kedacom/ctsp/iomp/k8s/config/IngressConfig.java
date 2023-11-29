package com.kedacom.ctsp.iomp.k8s.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ops 平台全局配置
 * <p>
 * 配置不同部署地点的ops平台的个性信息
 */
@Component
@ConfigurationProperties(
        prefix = "ingress"
)
@Data
public class IngressConfig {

    String sinppetHttps = "if ($scheme = http ) {\n" +
            "return 301 http://%s/dops/static/download/crt.html?type=jumpLink;\n" +
            "}\n";

    String sinppetHttp = "if ($scheme = https ) {\n" +
            "return 301 http://$host$request_uri;\n" +
            "}";
}
