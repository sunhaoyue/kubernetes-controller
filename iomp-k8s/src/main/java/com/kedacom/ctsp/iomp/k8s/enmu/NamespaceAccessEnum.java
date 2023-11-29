package com.kedacom.ctsp.iomp.k8s.enmu;

public enum NamespaceAccessEnum {

    PRIVATE("private",0),

    PUBLIC("public",1);

    private String label;

    private int value;

    NamespaceAccessEnum(String label, int value){
        this.label = label;
        this.value = value;
    }

    public String getLabel(){
        return this.label;
    }

    public int getValue(){
        return this.value;
    }
}
