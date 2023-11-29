package com.kedacom.ctsp.iomp.k8s.Entity;

import lombok.Data;

/**
 * @author sunhaoyue
 * @date Created in 2023/11/28 14:03
 */
@Data
public class UpgradeDBSource {
    /**
     * 集群名称  mysql/sqlite
     */
    private String dbType;

    /**
     * (拼接好url地址)
     * jdbc:mysql://${PLATFORM_MYSQL_IP}:${PLATFORM_MYSQL_PORT}/iomp?useUnicode=true&characterEncoding=utf-8&serverTimezone=UTC&useSSL=false
     * <p>
     * jdbc:sqlite:/data/dolphin/sqlite/iomp.db
     */
    private String url;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;
}
