package com.kedacom.ctsp.iomp.k8s.exec;

import com.alibaba.fastjson.JSONObject;
import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import com.kedacom.ctsp.iomp.k8s.K8sConfig;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.internal.ExecWebSocketListener;
import io.fabric8.kubernetes.client.utils.InputStreamPumper;
import net.bytebuddy.implementation.bytecode.Throw;
import okhttp3.Response;
import org.junit.Before;
import org.junit.Test;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExecuteMultiCmdTest {
    private K8sConfig k8sConfig;
    private K8sClientHolder clientHolder;
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    @Before
    public void setup() {
        k8sConfig = new K8sConfig();
        k8sConfig.setProtocol("https");
        k8sConfig.setMasters(Arrays.asList("10.165.141.2:6443"));
        k8sConfig.setCaCertFile("/data/iomp_data/kube/ca.crt");
        k8sConfig.setClientCertFile("/data/iomp_data/kube/apiserver-kubelet-client.crt");
        k8sConfig.setClientKeyFile("/data/iomp_data/kube/apiserver-kubelet-client.key");

       /* k8sConfig.setMasters(Arrays.asList("10.68.7.96:6443"));
        k8sConfig.setCaCertFile("/Users/hesen/work/keda/crt/cluster96/ca.crt");
        k8sConfig.setClientCertFile("/Users/hesen/work/keda/crt/cluster96/apiserver-kubelet-client.crt");
        k8sConfig.setClientKeyFile("/Users/hesen/work/keda/crt/cluster96/apiserver-kubelet-client.key");
*/
        clientHolder = new K8sClientHolder(k8sConfig);
    }


    @Test
    public void testPodPipes() {
        String namespace = "dp-system";
        String podName = "iomp-command-5c49bd7b46-4jghr";
        String[] cmds = getCreatDbCmds();
        try {

            KubernetesClient client = clientHolder.getClient();
            ExecWatch watch = client.pods().inNamespace(namespace).withName(podName)
                    .redirectingInput()
                    .redirectingOutput()
                    .redirectingError()
                    .redirectingErrorChannel()
                    .withTTY()
                    .usingListener(new SimpleListener())
                    .exec();

            String s = exec(watch,cmds);
            System.out.println(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static CompletableFuture<Boolean> pump(InputStream in, InputStreamPumper.Writable out, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                InputStreamPumper.transferTo(in, out);
            } catch (Exception e) {

            }
            return true;
        }, executor);
    }


    private String exec(ExecWatch watch, String[] cmds) {

        StringBuilder result = new StringBuilder();
        AtomicBoolean flag = new AtomicBoolean();

        CompletableFuture<Boolean> pump = pump(watch.getOutput(), (b, o, l) -> {
                    // 这里获取的字符串 并不是执行一行命令输出一行。而是执行多行后，一起输出的字符串
                    String s = new String(b, o, l);

                    int count = StringUtils.countMatches("s","Command execution End");
                    // 若获取字符串直接到结束（结束符号出现两次）则直接返回字符串
                    if (count == 2) {
                        result.append(s);
                        throw new IOException();
                    } else {
                        result.append(s);

                        if(!flag.get()){
                            flag.set(true);
                        }else {
                            throw new IOException();
                        }
                    }

                },
                executorService);

        try {


            for (int i = 0; i < cmds.length; i++) {
                watch.getInput().write((cmds[i] + "\n").getBytes());
                watch.getInput().flush();
            }


            watch.getInput().write(("exit;\n").getBytes());
            watch.getInput().flush();

            watch.getInput().write(("echo Command execution End\n").getBytes());
            watch.getInput().flush();

            pump.get(60, TimeUnit.SECONDS);

        } catch (java.io.IOException e) {
            e.printStackTrace();
        } catch (java.lang.InterruptedException e){
            e.printStackTrace();
        }catch (TimeoutException e){
            e.printStackTrace();
        }catch (java.util.concurrent.ExecutionException e){
            e.printStackTrace();
        }

        return result.toString();
    }


    @Test
    public void testExecTTy() {
        String namespace = "dp-system";
        String podName = "iomp-command-5bb89598bb-rv5vk";
        String[] cmds = getCreatDbCmds();
        try (
                KubernetesClient client = clientHolder.getClient();
                ExecWatch watch = client.pods().inNamespace(namespace).withName(podName)
                        .readingInput(System.in)
                        .writingOutput(System.out)
                        .writingError(System.err)
                        .withTTY()
                        .usingListener(new SimpleListener())
                        .exec("sh", "-c", "ls ", "-al")

        ) {
            for (int i = 0; i < 10; i++) {
                System.out.println("-----------" + i);
                Thread.sleep(1000L);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String[] getCreatDbCmds() {

        return new String[]{"mysql  -P3306 -hdol-mysql.kedacom-project-namespace -uroot -p807hBQNk5Phe",
                "CREATE DATABASE iomp_v3;",
                "CREATE USER 'iomp_v3'@'%' IDENTIFIED BY '83Ac0d76b737a8d2@';",
                "GRANT USAGE ON *.* TO iomp_v3@'%'; ",
                "GRANT ALL PRIVILEGES ON iomp_v3.* TO iomp_v3@'%'; ",
                "GRANT USAGE ON *.* TO iomp_v3@'%'  identified by '83Ac0d76b737a8d2@';"};

    }

    private List<String> getDataSourceCmds() {

        return Arrays.asList("mysql  -P3306 -hdol-mysql.kedacom-project-namespace -usystem -pUErBTi1zRJgv",
                "CREATE DATABASE iomp_v3;",
                "CREATE USER 'iomp_v3'@'%' IDENTIFIED BY '83Ac0d76b737a8d2@';",
                "GRANT USAGE ON *.* TO iomp_v3@'%'; ",
                "GRANT ALL PRIVILEGES ON iomp_v3.* TO iomp_v3@'%'; ",
                "GRANT USAGE ON *.* TO iomp_v3@'%'  identified by '83Ac0d76b737a8d2@';");

    }


    private static class SimpleListener implements ExecListener {

        @Override
        public void onOpen(Response response) {
            System.out.println("The shell will remain open for 10 seconds.");
        }

        @Override
        public void onFailure(Throwable throwable, Response response) {
            System.err.println("shell barfed");
        }

        @Override
        public void onClose(int i, String s) {
            System.out.println("The shell will now close.");
        }
    }

    private static class FutureChecker implements Runnable {
        private final String name;
        private final Future<?> future;

        private FutureChecker(String name, Future<?> future) {
            this.name = name;
            this.future = future;
        }

        @Override
        public void run() {
            if (!future.isDone()) {
                System.out.println("Future:[" + name + "] is not done yet");
            }
        }
    }

}
