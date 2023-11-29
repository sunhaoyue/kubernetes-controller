package com.kedacom.ctsp.iomp.k8s;

/**
 * 7层协议枚举
 *
 * @author hefei
 * @create 2018-04-05 1:36 PM
 **/
public enum Net7Protocol {

    HTTP("HTTP"),
    HTTPS("HTTPS"),
    TCP("TCP"),
    UDP("UDP"),
    BGP("BGP"),
    HTTP_HTTPS("HTTP&HTTPS");

    private String label;

    Net7Protocol(String label){
        this.label = label;
    }

    public String getLabel(){
        return this.label;
    }

}