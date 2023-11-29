package com.kedacom.ctsp.iomp.k8s;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kedacom.ctsp.iomp.k8s.operator.K8sDeployOperator;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.PodResource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author tangjixing
 * @date 2019/11/7
 */
@Slf4j
public class K8sOperatorTest {

    private K8sConfig k8sConfig;
    private K8sClientHolder clientHolder;


    private String namespace = "kedacom-project-namespace";

    @Before
    public void setup() {
        k8sConfig = new K8sConfig();
        k8sConfig.setProtocol("https");
        k8sConfig.setMasters(Arrays.asList("10.68.7.151:6443", "10.68.7.152:6443", "10.68.7.153:6443"));
        k8sConfig.setCaCertFile("D:\\k8s\\151_new/ca.crt");
        k8sConfig.setClientCertFile("D:\\k8s\\151_new/apiserver-kubelet-client.crt");
        k8sConfig.setClientKeyFile("D:\\k8s\\151_new/apiserver-kubelet-client.key");

        clientHolder = new K8sClientHolder(k8sConfig);
    }

    @Test
    public void testMasterTurn() throws JsonProcessingException {
        K8sDeployOperator deployOperator = new K8sDeployOperator(clientHolder);
        VersionInfo versionInfo = deployOperator.getVersionInfo();
        log.info("versionInfo={}", new ObjectMapper().writeValueAsString(versionInfo));
        assertNotNull(versionInfo);
        assertEquals(versionInfo.getGitVersion(), "V1.13.3");
    }

    @Test
    public void tesSecretByLabels() throws JsonProcessingException {
        DefaultKubernetesClient client = clientHolder.getClient();
        Map<String, String> map = new HashMap<>();
        Secret secret = client.secrets().withName("aaaaaaaaaaaa-urigsd").get();
        System.out.println(secret);

        SecretList secretList = client.secrets().inAnyNamespace().withLabelIn("cert/type", "service", "domain").list();
        for (Secret item : secretList.getItems()) {
            System.out.println(item.getMetadata().getName());
        }
//        SecretList list = client.secrets().list();
//        System.out.println(list.getItems());
    }

    @Test
    public void testCpFileFromPod() throws Exception {
        PodResource<Pod> podResource =
                clientHolder.getClient().pods().inNamespace(namespace).withName("dol-mysql-0");
        System.out.println(podResource.file("/entrypoint.sh ").copy(Paths.get("D:/data/entrypoint.sh")).booleanValue());
    }


    @Test
    public void testCpFileToPod() throws Exception {
        // 这个没有验证通过，暂时还无法处理
        PodResource<Pod> podResource =
                clientHolder.getClient().pods().inNamespace(namespace).withName("dol-mysql-0");
        FileInputStream inputStream = new FileInputStream("D:/data/abc.txt");
        InputStream inputStream1 = podResource.readingInput(inputStream).exec("sh", "-c", "read temp_file; echo '${temp_file}' > /home/abc.txt")
                .getOutput();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"))) {
            String line = reader.readLine();
            if (line != null) {
                System.out.println(line);
            }
        }
    }



    @Test
    public void testPodExec1() throws Exception {

        ExecWatch exec = clientHolder.getClient().pods().inNamespace(namespace).withName("dol-mysql-0").exec("ls -l");
        System.out.println("执行完成。");
        try (InputStream output = exec.getOutput();
             BufferedReader reader = new BufferedReader(new InputStreamReader(output, "UTF-8"));) {
            String line = reader.readLine();
            while (line != null) {
                System.out.println(line);
                line = reader.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testPodExec() throws Exception {

        String dbName = "kedatest1";
        String username = "kedacom1";
        String password = "kedacom121";

        List<String> cmds = new ArrayList<>();
        cmds.add("mysql -uroot -p${MYSQL_ROOT_PASSWORD}; ");
        cmds.add("CREATE DATABASE " + dbName + ";");
        cmds.add("CREATE USER '"+username+"'@'%' IDENTIFIED BY '"+password+"'; ");
        cmds.add("GRANT USAGE ON *.* TO '"+username+"'@'%';  ");
        cmds.add("GRANT ALL PRIVILEGES ON " +dbName+".* TO '"+username+"'@'%';  ");
        cmds.add("GRANT USAGE ON *.* TO '"+ username +"'@'%'  identified by '"+password+"';");



        new K8sDeployOperator(clientHolder).execShellInPod("dol-mysql-0", namespace, cmds, message -> {
            System.out.println(message);
        });


    }


}
