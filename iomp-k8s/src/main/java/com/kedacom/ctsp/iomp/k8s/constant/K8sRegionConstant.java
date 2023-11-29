package com.kedacom.ctsp.iomp.k8s.constant;

public interface K8sRegionConstant {

    String SIGN = "sign";

    String CREATE_TIME = "createTime";

    String NAME = "name";

    String IPS = "ips";

    String NAMESPACES = "namespaces";

    String USER_ROLES = "userRoles";

    /**
     * 域类型
     * common：公共域  只打label
     * personal： 私有域  打label和taint
     */
    String TYPE = "type";

    String EFFECT = "effect";

    String DEPARTMENT = "department";

    /**
     * centos, centos_arm, uos, kylin
     */
    String SYSTEM_TYPE = "systemType";

    /**
     * os cpu 架构
     * linux/amd64
     * linux/arm64
     */
    String OS_ARCH = "osArch";

    String COMMON_NAME = "默认域";

    String COMMON_TYPE = "common";

    String PERSONAL_TYPE = "personal";

}
