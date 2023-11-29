package com.kedacom.ctsp.iomp.k8s.enmu;

public enum ServiceAccountEnum {

    /**
     *
     */
    ADMIN("admin"),

    /**
     *
     */
    CLUSTER_READ("cluster-read"),

    /**
     *
     */
    NS_ADMIN("ns-admin"),

    /**
     *
     */
    NS_READ("ns-read"),
    /**
     *
     */
    DEFAULT("default");


    private String value;

    ServiceAccountEnum(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

}
