package com.kedacom.ctsp.iomp.k8s;

/**
 * kubernetes unavailable exception
 * <p>
 * Created by hefei on 2016/1/26.
 */
public class K8sUnavailableException extends RuntimeException {


    private static final long serialVersionUID = -6002452207336889089L;

    public K8sUnavailableException() {
    }

    public K8sUnavailableException(String message) {
        super(message);
    }

    public K8sUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public K8sUnavailableException(Throwable cause) {
        super(cause);
    }

    public K8sUnavailableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
