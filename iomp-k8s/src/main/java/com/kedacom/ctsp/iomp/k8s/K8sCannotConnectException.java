package com.kedacom.ctsp.iomp.k8s;


import com.kedacom.ctsp.lang.exception.CommonException;

/**
 * K8s所有的api server 无法连接
 */
public class K8sCannotConnectException extends CommonException {
    public K8sCannotConnectException(String message) {
        super(message);
    }

    public K8sCannotConnectException(String message,Object... args) {
        super(message,args);
    }
}
