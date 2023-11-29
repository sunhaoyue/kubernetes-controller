package com.kedacom.ctsp.iomp.k8s;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kedacom.ctsp.iomp.k8s.operator.K8sConfigmapOperator;
import com.kedacom.ctsp.iomp.k8s.operator.K8sDeployOperator;
import com.kedacom.ctsp.iomp.k8s.operator.K8sDeploymentOperator;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.client.VersionInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.RegEx;
import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author tangjixing
 * @date 2019/11/7
 */
@Slf4j
public class K8sApiTest {

    private K8sConfig k8sConfig;
    private K8sClientHolder clientHolder;

    @Before
    public void setup() {
        k8sConfig = new K8sConfig();
        k8sConfig.setK8sName("test");
        k8sConfig.setProtocol("https");
        k8sConfig.setMasters(Arrays.asList("10.165.24.5:6443","10.165.24.6:6443","10.165.24.7:6443"));
        k8sConfig.setCaCertFile("E:/data/k8s/ca.crt");
        k8sConfig.setClientCertFile("E:/data/k8s/apiserver-kubelet-client.crt");
        k8sConfig.setClientKeyFile("E:/data/k8s/apiserver-kubelet-client.key");

        clientHolder = new K8sClientHolder(k8sConfig);
        clientHolder.initCache();
    }


    @Test
    public void headlessService() {

        Map<String, String> selector = new HashMap<>();
        selector.put("client_id", "name");

        final ServicePort tcp = new ServicePortBuilder().withPort(9080)
                .withTargetPort(new IntOrString(9080))
                .withName("test-headless").withProtocol("TCP").build();
        final Service service = new ServiceBuilder().editOrNewMetadata().withName("test-headless")
                .endMetadata()
                .editOrNewSpec()
                .withClusterIP("None")
                .withPorts(tcp).withSelector(selector).endSpec().build();

        log.info(JSON.toJSONString(service));

        clientHolder.getClient().services().inNamespace("default").createOrReplace(service);
    }

    @Test
    public void createSecret() {

        Secret secret = new Secret();
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("tls.key","LS0tLS1CRUdJTiBSU0EgUFJJVkFURSBLRVktLS0tLQpNSUlFcEFJQkFBS0NBUUVBbk0zQWgzZWZuNlNySHBIWitGYTZ2WGtXRlN2Q0FqNXV0NlIyTEo1OFZTZG1uWWh2Cmlkd0FieEgxck5jMFdFeXJ3TFF6QWZ0TzlJS1pwZEs1eUsydWs0dC9Jc2lwaHAxUVhXRWt4NWsrelp3RUNxaVYKZHZ0ekJDN0dIbG1vSFQxeFlDWXpLQ3pNZlY1VlZ0N21WQlZYOXdvbEdSTWJJbEM0cXI0M3RlZC9iVFhTMXRmRQpzbVp4Njl1WVZoYXcyZElaVExpVk1VdWR5TUVTZWdPb2tqYlpWdi81WmRuMDJ1S2xaR3puYnFIbkwyb0lVeVo1Cjh3S2xJQ0tWNmlEV2ZsdFo0b1Zrd3A1QzhxV1k0SS9BRVRaZ1JYaE1GMnh0ZC9QSi9QNXVRU3d0eWp1RWcrbkIKRTBuZXpSSktPbEdnZ3BkK1FHcnExN0l6UGRmK3NicmtrMTlDb3dJREFRQUJBb0lCQURTenB0RWhNS0pNaTBNVQpER20rWkxkSUdsYjFSUmpSK1E1NkZVbjczcGdVVFJZSGhFMldod0xOeTMvVE9RR3dpMDJTZzA1WU0vcFVadVhvCkJJOWhhTHFvZVF1czIrV2x2QndXaVhFWW5aWW5xT2dZTDF3MU9Ud2ZxSEVNKzBjUW5xbU5UVVprZ0RwbTJWSUEKU3ZQWjFKM3daL1djaVZNb1JGa0c0OXljdVNSd0d3U2s0MFk4WGVvMDEyclNBSzFrZE9JTnZaRzBuN1JYL3QyLwpvTGNGZkh3NFkzV0dTcW9jZS9KaTYweDgraXpqaHBhc0srNkhxNEpRQzlVd2ZYd1c1QVBxQmZyNU10ZXRBbHYvCkZETWVwZGJXcHVhazMyaDI4WkhqVGNQRHVXQVY2WFFIVE5KcXFDdzRkclpPdkw2dFhnOHZCRU1JZ2o0TTA4a3IKdnNmcGM0a0NnWUVBeWlWSTQwdXlLeHNCWjVLRFNxaXdTRStxUnJEV1N6NExFUVRmekdlK1p6czZZWm5tZ3loQgpKU21rdXEzdGtCV0ZpTStzYlo2Ujc0V1czcHVRMFBGZTByMjhldWZhOWJpNEx4N29pek5lOURPODMyS1hHVGVFCkREdkNxRXhjMFZicFZxcGZObzhmd016d0o5S3V4TXV5bGZhSWlsVlhja1ZVU1lHS0IyQmluVWNDZ1lFQXhwUVAKOU5ycThmcTl2blB5cU9YdmdFZFpSZ2trd05ZQW5meXIyRWJOaG5aU00ycm95Tkx3UmxwaVRKUzVKT0l6OCs0Swo1aVNocnprdVZLM1RQMmtSM1dlNlNGR3FvVTFRbFlaMDZsZm1DQmNlZTVvN28wb28vRWllN09tcm5DUk1qRGpQCmRqMHV5MGFkTGFoZHRNYU1QdFQ5QW5sbGd6MUNKaitBckcvVGJjVUNnWUFsOGVYQ3ROelc5cFRHNmx1MjBmOTEKTTI4VklmQzM0d2VVeEVOMlRTc1NtYTJWMEp2U2x1WFRvZ09ILzBvT3Q5dC9HT3lYRHlMNXdTdlcwWURYbDlkaQoyN2JibzFZWXRmbnM3bkpjWHVJK0dOQWxabzVTYjNkY0RJTzNyODNraGRuN0tMUUN1ODhNRSt3b3JZV2M3MWV3CmlyeUtxd1psTHRwcGllVnRDUXk1MVFLQmdRQzhXdWJEdHY3UlphZWl4Zkl1dUdNelJ2bnJ6M0o0SXNUVkZqeWQKMlpMSzd1SkxlU1d5anpwdlVQNGFhNXN0M0EyeFcySWxLQ3ZndTVreG91dFVJMEpad3pEVHRmcm1JeUxEb1pTMgpUNGVXdHU4b0NJUjIvem5mQ3JjTU94eVc3MnRZT2U2MjFaUFVKbmVpUGlnYjk1UkJhTjlRQUh3RVB3L0duY2RjClNIbkFqUUtCZ1FDVU5yTlBZeE1vcm9ISkVkbit3OGl3czQ5TDdOdnpqRzNVdlNGajRiTlNaQ1pQdG5vVUpPMkIKa1F1VWFnaUh3N0hHbnNyZmJSc05vYjl6cWlaYnJXRys4ck01UDl4elYrZjNzQmhZYkNrTnM0RUMwTW1DWGs0cQpQWG9iL2hyTk9KeGQ1YTFUTzkyZGdqU1pONWpXMCtRNDJ4c1R3V3B1UXZPZDBOZGNWc3BTQVE9PQotLS0tLUVORCBSU0EgUFJJVkFURSBLRVktLS0tLQo=");
        dataMap.put("tls.crt", "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURZVENDQWttZ0F3SUJBZ0lCVURBTkJna3Foa2lHOXcwQkFRc0ZBREJ0TVFzd0NRWURWUVFHRXdKRFRqRVEKTUE0R0ExVUVDQXdIU21saGJtZFRkVEVOTUFzR0ExVUVDZ3dFYVc5dGNERU5NQXNHQTFVRUN3d0VhVzl0Y0RFTgpNQXNHQTFVRUF3d0VhVzl0Y0RFZk1CMEdDU3FHU0liM0RRRUpBUllRYVc5dGNFQnJaV1JoWTI5dExtTnZiVEFlCkZ3MHlNREExTWpZeE1URTFORGRhRncwek1EQTFNalF4TVRFMU5EZGFNRTh4Q3pBSkJnTlZCQVlUQWtOT01Rc3cKQ1FZRFZRUUlEQUpLVXpFUU1BNEdBMVVFQ2d3SGEyVmtZV052YlRFTk1Bc0dBMVVFQ3d3RWNISnZaREVTTUJBRwpBMVVFQXd3Sk1UQXVPUzQ1TGpjMk1JSUJJakFOQmdrcWhraUc5dzBCQVFFRkFBT0NBUThBTUlJQkNnS0NBUUVBCm5NM0FoM2VmbjZTckhwSForRmE2dlhrV0ZTdkNBajV1dDZSMkxKNThWU2Rtbllodmlkd0FieEgxck5jMFdFeXIKd0xRekFmdE85SUtacGRLNXlLMnVrNHQvSXNpcGhwMVFYV0VreDVrK3pad0VDcWlWZHZ0ekJDN0dIbG1vSFQxeApZQ1l6S0N6TWZWNVZWdDdtVkJWWDl3b2xHUk1iSWxDNHFyNDN0ZWQvYlRYUzF0ZkVzbVp4Njl1WVZoYXcyZElaClRMaVZNVXVkeU1FU2VnT29ramJaVnYvNVpkbjAydUtsWkd6bmJxSG5MMm9JVXlaNTh3S2xJQ0tWNmlEV2ZsdFoKNG9Wa3dwNUM4cVdZNEkvQUVUWmdSWGhNRjJ4dGQvUEovUDV1UVN3dHlqdUVnK25CRTBuZXpSSktPbEdnZ3BkKwpRR3JxMTdJelBkZitzYnJrazE5Q293SURBUUFCb3lvd0tEQUpCZ05WSFJNRUFqQUFNQnNHQTFVZEVRUVVNQktICkJBcEVCNWVIQkFwRUI1aUhCQXBFQjVrd0RRWUpLb1pJaHZjTkFRRUxCUUFEZ2dFQkFORXZkaEV5VFBJRVB5Z0UKc1l5UjcwdU94QlVLRGp6bG1mR0grN1daOTkyOHBqNlVrL2hyMXJwN3p6TXFUUGJnaDlYRkhzam1OUENKWmhpVwpWaE5XM3o3RXNCZlBURk5uOE1saWxIYnpTa0RGbGtCTVlQaU01VXJxa3BEaWVXTEtWZ3hiV00zdUpvYkVXcWxyCkh6dXl0WDNCZDhaZXVHSmlycTNCRTZYRGRnZnYzQURYQ09XK0ltY25YcHc3UkhjZCtrbmFMR2p4TnVUTCs5VkoKWElmbHV0K1p6SEpsMUhGOXJ0dXM1WHNUTGs4eHkxUENrK0EwR3NzQzBibGlTUE93bHoyT01wTEQrQTRmek85eQpUUkczc0VtYlV4NTlVM0IreWdGUXhVa1EyT3dPQnNtcDFLejdhbWFlTzlHV2JuQ1JWcWtUVFBoVTl5M1ZNUnBwCmxuY1dqYkk9Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K");
        dataMap.put("certFormat", "aaaa");
        secret.setData(dataMap);
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName("dops-secret");
        metadata.setNamespace("kedacom-project-namespace");
        Map<String, String> labels  = new HashMap<>();
        labels.put("type", "ingress");
        labels.put("client_id", "dops");
        metadata.setLabels(labels);
        Map<String, String> annotations  = new HashMap<>();
        annotations.put("hosts", "10.68.7.151,10.68.7.152,10.68.7.153");
        annotations.put("vip", "");
        metadata.setAnnotations(annotations);
        secret.setMetadata(metadata);
        secret.setType("kubernetes.io/tls");

        Secret orReplace = clientHolder.getClient().secrets().inNamespace("kedacom-project-namespace").createOrReplace(secret);
        System.out.println(orReplace);

    }

    @Test
    public void testOne() throws InterruptedException {
        K8sConfigmapOperator operator = new K8sConfigmapOperator(clientHolder);
        long s = System.currentTimeMillis();
        List<? extends HasMetadata> list = operator.getK8sCacheOperator().list();
        System.out.println("第一次耗时：" + (System.currentTimeMillis() - s));
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    long st = System.currentTimeMillis();
                    List<? extends HasMetadata> d = operator.getK8sCacheOperator().list();
                    new K8sDeploymentOperator(clientHolder).getK8sCacheOperator().list();
                    System.out.println("index["+ j +"]耗时：" + (System.currentTimeMillis() - st));
                }
            }).start();

        }

        System.out.println("complete");
        TimeUnit.SECONDS.sleep(100);
        clientHolder.destroy();
        System.out.println("destory");



        TimeUnit.SECONDS.sleep(1000);
    }
    @Test
    public void test2(){
        long s = System.currentTimeMillis();
        K8sDeploymentOperator operator = new K8sDeploymentOperator(clientHolder);
        List<Deployment> result = operator.list(100L);
        log.info("result => {}" , JSON.toJSONString(result));
        System.out.println("第一次耗时：" + (System.currentTimeMillis() - s));

        long s2 = System.currentTimeMillis();
        DeploymentList deploymentLists = clientHolder.getClient().apps().deployments().list(new ListOptionsBuilder()
                .withLimit(5L)
                .withContinue(null)
                .build());
        System.out.println("第二次耗时：" + (System.currentTimeMillis() - s2));
//        log.info("one => {}" , JSON.toJSONString(deploymentLists));
        deploymentLists = clientHolder.getClient().apps().deployments().list(new ListOptionsBuilder()
                .withLimit(1L)
                .withContinue(deploymentLists.getMetadata().getContinue())
                .build());
//        log.info("two => {}" , JSON.toJSONString(deploymentLists));
        deploymentLists = clientHolder.getClient().apps().deployments().list(new ListOptionsBuilder()
                .withLimit(1L)
                .withContinue(deploymentLists.getMetadata().getContinue())
                .build());
//        log.info("three => {}" , JSON.toJSONString(deploymentLists));
    }

    @Test
    public void test3(){

    }

    @Test
    public void testReg() {
        final String PARAM_REG = "-\\s*(\".+?\"|[^:\\s])+(?=[:\\s])";
        final Pattern compile = Pattern.compile(PARAM_REG);
        final Matcher matcher = compile.matcher("EXEC mysql createdb -f -dname=dol-mysql -db-name=iomp -username-mp=admin -password=kedacom123");
        while (matcher.find()) {
            System.out.println(matcher.group());
        }

/*
        if (!matcher.matches()) {
            System.out.println("不匹配");
        }

        if (matcher.find()) {
            System.out.println(matcher.groupCount());
            System.out.println(matcher.group(0));
            System.out.println(matcher.group(1));
            System.out.println(matcher.group(2));
        }*/


    }


    public static void main(String[] args) {

        String prefix = "cconfig.config.";
        String c = "cconfig.config.spring.datasource.password";
        System.out.println(c.indexOf(prefix));

        System.out.println(c.indexOf(prefix));

        String s = "abcdefg";
        System.out.println(s.substring(1, 1));

    }



}
