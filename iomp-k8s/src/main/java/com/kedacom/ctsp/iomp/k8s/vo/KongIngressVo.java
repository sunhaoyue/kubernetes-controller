package com.kedacom.ctsp.iomp.k8s.vo;

import lombok.Data;

import java.util.List;

/**
 * kongIngress信息(路由配置信息，是部署时使用的部署信息)
 *
 * @author hongkai
 * @create 2018-01-13 上午9:13
 **/
@Data
public class KongIngressVo {

    private String nameSpace;

    private String projectMark;

    private String projectSign;

    private String name;

    private List<String> methods;

    private Boolean stripPath;

    private List<String> protocols;

    private Integer regexPriority;

    public KongIngressVo(String nameSpace, String name, List<String> methods, Boolean stripPath, List<String> protocols, Integer regexPriority,String projectMark,String projectSign) {
        this.nameSpace = nameSpace;
        this.name = name;
        this.methods = methods;
        this.stripPath = stripPath;
        this.protocols = protocols;
        this.regexPriority = regexPriority;
        this.projectMark=projectMark;
        this.projectSign = projectSign;
    }
}
