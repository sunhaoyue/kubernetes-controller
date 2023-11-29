package com.kedacom.ctsp.iomp.k8s.constant;

import com.google.common.collect.Maps;

import java.util.Map;

public class K8sPriorityClassConstant {

    public static final String L1_NAME = "dolphin-level-1";
    public static final String L2_NAME = "dolphin-level-2";
    public static final String L3_NAME = "dolphin-level-3";
    public static final String L4_NAME = "dolphin-level-4";
    public static Map<String, Integer> priorityMap;

    static {
        priorityMap = Maps.newHashMap();
        priorityMap.put(L1_NAME, 3000);
        priorityMap.put(L2_NAME, 2000);
        priorityMap.put(L3_NAME, 1900);
        priorityMap.put(L4_NAME, 1800);
    }
}
