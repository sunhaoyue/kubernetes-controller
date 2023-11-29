package com.kedacom.ctsp.iomp.k8s.enmu;

import com.kedacom.ctsp.iomp.k8s.constant.K8sPriorityClassConstant;

public enum K8sPriorityClassEnum {

    L1(K8sPriorityClassConstant.L1_NAME),


    L2(K8sPriorityClassConstant.L2_NAME),


    L3(K8sPriorityClassConstant.L3_NAME),


    L4(K8sPriorityClassConstant.L4_NAME);

    private String name;

    K8sPriorityClassEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
