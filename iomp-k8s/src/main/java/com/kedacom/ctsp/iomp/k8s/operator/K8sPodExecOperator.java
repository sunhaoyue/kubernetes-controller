package com.kedacom.ctsp.iomp.k8s.operator;

import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.PodResource;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * @author tangjixing
 * @date 2020/4/29
 */
@Slf4j
public class K8sPodExecOperator extends K8sPodOperator {

    public K8sPodExecOperator(K8sClientHolder k8sClientHolder) {
        super(k8sClientHolder);
    }


    /**
     * 一行行命令进行执行
     *
     * @param name
     * @param namespace
     * @param command
     */
    public void exec(String name, String namespace, List<String> command) {
        // 这里考虑要有日志输出

        PodResource<Pod> podResource =
                getK8sClientHolder().getClient().pods().inNamespace(namespace).withName(name);

        try (ExecWatch exec = podResource.redirectingInput().withTTY().exec("sh")) {
            for (String cmd : command) {
                exec.getInput().write((cmd + " \n").getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
