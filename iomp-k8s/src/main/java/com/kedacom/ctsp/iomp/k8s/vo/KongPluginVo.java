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
public class KongPluginVo {

    private String nameSpace;

    private String name;

    private String projectMark;

    private List<String> whiteList;

    private List<String> blackList;


    public KongPluginVo(String nameSpace, String name, List<String> whiteList, List<String> blackList,String projectMark) {
        this.nameSpace = nameSpace;
        this.name = name;
        this.whiteList = whiteList;
        this.blackList = blackList;
        this.projectMark=projectMark;
    }
}
