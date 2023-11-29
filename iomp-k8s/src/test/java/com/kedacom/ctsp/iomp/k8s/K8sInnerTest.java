package com.kedacom.ctsp.iomp.k8s;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.kedacom.ctsp.iomp.k8s.common.util.YamlUtil;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import lombok.extern.slf4j.Slf4j;
import okhttp3.internal.http2.ErrorCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.kedacom.ctsp.iomp.k8s.operator.K8sNamespaceOperator.LABEL_REGION;

/**
 * @author tangjixing
 * @date 2019/11/7
 */
@Slf4j
public class K8sInnerTest {

    private K8sConfig k8sConfig;
    private K8sClientHolder clientHolder;
    public static final String LF = "\n";

    @Before
    public void setup() {
        k8sConfig = new K8sConfig();
        k8sConfig.setProtocol("https");
        /*k8sConfig.setMasters(Arrays.asList("10.165.120.1:6443"));
        k8sConfig.setCaCertFile(readFile2() + "/data/prod/ca.crt");
        k8sConfig.setClientCertFile(readFile2() + "/data/prod/apiserver-kubelet-client.crt");
        k8sConfig.setClientKeyFile(readFile2() + "/data/prod/apiserver-kubelet-client.key");*/

        k8sConfig.setMasters(Arrays.asList("10.165.76.41:6443"));
        k8sConfig.setCaCertFile("E:\\data\\iomp_data\\ca\\2/ca.crt");
        k8sConfig.setClientCertFile("E:\\data\\iomp_data\\ca\\2/apiserver-kubelet-client.crt");
        k8sConfig.setClientKeyFile("E:\\data\\iomp_data\\ca\\2/apiserver-kubelet-client.key");
        clientHolder = new K8sClientHolder(k8sConfig);
    }

    public String readFile2() {
        return Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("temp")).getPath();

    }

    @Test
    public void getAllNamespace() {
        NamespaceList namespaces = clientHolder.getClient().namespaces().list();
        System.out.println(JSONObject.toJSONString(namespaces));
    }

    @Test
    public void testToYmal() {
        DeploymentList deploymentList = queryAllDeployment();
        Deployment deployments = deploymentList.getItems().get(0);
        System.out.println(convertObjToYaml(deployments));
    }

    /**
     * 对象转yaml字符串
     * <p>
     *
     * @param obj Object 要转换的对象
     * @return
     */
    public String convertObjToYaml(Object obj) {
        Yaml yaml = new Yaml();
        StringWriter sw = new StringWriter();
        yaml.dump(obj, sw);
        return sw.toString();
    }
    @Test
    public void test() {
        DeploymentList deploymentList = queryAllDeployment();
        List<Deployment> deployments = deploymentList.getItems();
        int count = 1;
        for (Deployment deployment : deployments) {
            // 转成Yaml字符串
            String objToYaml = YamlUtil.convertObjToYaml(deployment);
            StringBuffer sb = new StringBuffer();
            String[] split = StringUtils.split(objToYaml, LF);
            for (String line : split) {
                if (StringUtils.startsWithIgnoreCase(line.trim(), "additionalProperties")) {
                    continue;
                }
                sb.append(line + LF);
            }
            try {
                File valuesFile = YamlUtil.convert2File(StringUtils.removeEnd(sb.toString(), LF), "/data/iomp_data/yamlfile/" + deployment.getMetadata().getName() + count + ".yaml");
                if (!valuesFile.exists()) {
                    throw new RuntimeException("values文件生成失败");
                }
            }catch (IOException e){
                e.printStackTrace();
            }


          /*  List<Container> containers = deployment.getSpec().getTemplate().getSpec().getContainers();
            if (CollectionUtils.isNotEmpty(containers)) {
                for (Container container : containers) {
                    if (StringUtils.equals(container.getImage(), "harbor.bestkunlun.com/test/fluent-bit:1.2.1")) {
                        System.out.println("udpate:" + deployment.getMetadata().getName() + "," + deployment.getMetadata().getNamespace());
                        container.setImage("harbor.bestkunlun.com/test/fluent-bit:1.2.1-1.0.0");
                        clientHolder.getClient().apps().deployments().replace(deployment);
                    }
                }
            }*/
            count++;

        }

    }


    /**
     * 查询deployment
     */
    private DeploymentList queryAllDeployment() {
        DeploymentList deploymentList = null;
        try {
            deploymentList = clientHolder.getClient().apps().deployments().inAnyNamespace().list();
        } catch (Exception e) {
            log.error("PackageDeployer.queryAllDeployment ----------------- exception : {}", e.getMessage());
        }
        return deploymentList;
    }


}
