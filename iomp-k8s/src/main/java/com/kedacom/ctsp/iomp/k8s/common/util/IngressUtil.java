package com.kedacom.ctsp.iomp.k8s.common.util;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class IngressUtil {

    public static String getIngressClass(DaemonSet daemonSet) {
        if (daemonSet == null) {
            return null;
        }
        List<Container> containers = daemonSet.getSpec().getTemplate().getSpec().getContainers();
        String ingressClass = null;
        outer:
        for (Container c : containers) {
            for (String arg : c.getArgs()) {
                if (StringUtils.startsWith(arg, "--ingress-class")) {
                    ingressClass = StringUtils.substringAfter(arg, "=");
                    break outer;
                }
            }
        }
        return ingressClass;

    }

    public static String getControllerClass(DaemonSet daemonSet) {
        if (daemonSet == null) {
            return null;
        }
        List<Container> containers = daemonSet.getSpec().getTemplate().getSpec().getContainers();
        String ingressClass = null;
        outer:
        for (Container c : containers) {
            for (String arg : c.getArgs()) {
                if (StringUtils.startsWith(arg, "--controller-class")) {
                    ingressClass = StringUtils.substringAfter(arg, "=k8s.io/");
                    break outer;
                }
            }
        }
        return ingressClass;
    }


    public static String getTcpServiceConfigmap(DaemonSet daemonSet) {
        if (daemonSet == null) {
            return null;
        }
        List<Container> containers = daemonSet.getSpec().getTemplate().getSpec().getContainers();
        String tcpConfigmap = null;
        outer:
        for (Container c : containers) {
            for (String arg : c.getArgs()) {
                if (StringUtils.startsWith(arg, "--tcp-services-configmap")) {
                    tcpConfigmap = StringUtils.substringAfter(StringUtils.substringAfter(arg, "="), "/");
                    break outer;
                }
            }
        }
        return tcpConfigmap;
    }


    public static String getUdpServiceConfigmap(DaemonSet daemonSet) {
        if (daemonSet == null) {
            return null;
        }
        List<Container> containers = daemonSet.getSpec().getTemplate().getSpec().getContainers();
        String udpConfigmap = null;
        outer:
        for (Container c : containers) {
            for (String arg : c.getArgs()) {
                if (StringUtils.startsWith(arg, "--udp-services-configmap")) {
                    udpConfigmap = StringUtils.substringAfter(StringUtils.substringAfter(arg, "="), "/");
                    break outer;
                }
            }
        }
        return udpConfigmap;
    }


    public static String getIngressSecretName(DaemonSet daemonSet) {
        if (daemonSet == null) {
            return null;
        }
        List<Container> containers = daemonSet.getSpec().getTemplate().getSpec().getContainers();
        String secretName = null;
        outer:
        for (Container c : containers) {
            for (String arg : c.getArgs()) {
                if (StringUtils.startsWith(arg, "--default-ssl-certificate")) {
                    secretName = StringUtils.substringAfter(StringUtils.substringAfter(arg, "="), "/");
                    break outer;
                }
            }
        }
        return secretName;
    }
}
