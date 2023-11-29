//package com.kedacom.ctsp.iomp.k8s;
//
//import com.kedacom.ctsp.lang.exception.CommonException;
//import io.fabric8.kubernetes.api.model.Pod;
//import io.fabric8.kubernetes.client.KubernetesClientException;
//import io.fabric8.kubernetes.client.dsl.ExecListener;
//import io.fabric8.kubernetes.client.dsl.ExecWatch;
//import lombok.SneakyThrows;
//import lombok.extern.slf4j.Slf4j;
//import okhttp3.Response;
//import org.apache.commons.collections.CollectionUtils;
//import org.junit.Before;
//import org.junit.Test;
//
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Objects;
//import java.util.concurrent.*;
//
///**
// * @author hesen
// * @since 2021-10-2021/10/19
// */
//@Slf4j
//public class K8sExecTest {
//
//    private static final String DEFAULT_NAMESPACE = "kedacom-project-namespace";
//    private static final String LABEL_INSTANCE = "app.kubernetes.io/instance";
//    private static final String IOMP_COMMAND = "iomp-command";
//
//
//    private K8sClientHolder clientHolder;
//
//    private Pod pod;
//
//    @Before
//    public void setup() {
//        K8sConfig k8sConfig = new K8sConfig();
//        k8sConfig.setProtocol("https");
//        k8sConfig.setMasters(Arrays.asList("10.68.7.96:6443"));
//        k8sConfig.setCaCertFile("C:\\data\\k8s\\96\\ca.crt");
//        k8sConfig.setClientCertFile("C:\\data\\k8s\\96\\apiserver-kubelet-client.crt");
//        k8sConfig.setClientKeyFile("C:\\data\\k8s\\96\\apiserver-kubelet-client.key");
//        clientHolder = new K8sClientHolder(k8sConfig);
//
//        List<Pod> podList = clientHolder.getClient().pods().inNamespace(DEFAULT_NAMESPACE).withLabelIn(LABEL_INSTANCE, IOMP_COMMAND).list().getItems();
//        if (CollectionUtils.isEmpty(podList)) {
//            throw new CommonException("no such pod in label: key-> " + LABEL_INSTANCE + ",value-> " + IOMP_COMMAND);
//        }
//        pod = podList.stream().filter(pod -> Objects.equals(pod.getStatus().getPhase(), "Running")).findFirst().orElseThrow(() -> new RuntimeException("no running pod in label: key-> " + LABEL_INSTANCE + ",value-> " + IOMP_COMMAND));
//    }
//
//    /**
//     * exec
//     * <p>
//     * 1.如果脚本错误或者ExecWatch执行错误，程序会自动关闭ExecWatch，不需要手动关闭，否则需要手动关闭
//     * <p>
//     * 2.所有组合，输出结果为以下样式，影响输出结果的是withTTY
//     * ---------------------------
//     * sh-4.2# echo 'hello world'
//     * hello world
//     * sh-4.2#
//     * ---------------------------
//     * <p>
//     * 3.不使用withTTY和writingError，输出结果为以下样式，结果最后带有回车符\n，所以会空一行
//     * ---------------------------
//     * hello world
//     * <p>
//     * ---------------------------
//     * 4.不使用withTTY，某些情况，比如curl命令会输出错误流，错误流如下，标准输出流为空
//     * -----------------------------------------------------------------------------
//     * % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
//     * Dload  Upload   Total   Spent    Left  Speed
//     * 0     0    0     0    0     0      0      0 --:--:--  0:00:04 --:--:--     0
//     * -----------------------------------------------------------------------------
//     * 某些情况，如echo命令，只输出标准输出流，结果最后带有回车符\n，所以会空一行
//     * ---------------------------
//     * hello world
//     * <p>
//     * ---------------------------
//     * 5.如果未指定writingOutput或者redirectingOutput，获取ExecWatch.getOutput为null，其他流一样
//     */
//    @Test
//    public void execTest() throws InterruptedException {
//        ByteArrayInputStream in = new ByteArrayInputStream("echo 'hello world'\n".getBytes());
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        ByteArrayOutputStream err = new ByteArrayOutputStream();
//        ExecWatch watch = clientHolder.getClient().pods().inNamespace(DEFAULT_NAMESPACE).withName(pod.getMetadata().getName())
//                // 读取输入，这种方式只能读取输入流，如果通过ExecWatch.getInput则为null
//                .readingInput(in)
//                // 写输出到流，这种方式只能输出到指定流，如果通过ExecWatch.getOutput则为null
//                .writingOutput(out)
//                // 写错误输出到流，这种方式只能输出到指定流，如果通过ExecWatch.getError则为null
//                .writingError(err)
//                // 以终端的方式输出内容到指定流，包含输入、输出、错误
//                .withTTY()
//                // 指定监听器，监听开始、失败、完成事件
//                .usingListener(new SimpleListener())
//                // 执行指令
//                .exec("sh");
//
//        TimeUnit.SECONDS.sleep(5);
//        log.info("exec out: " + out.toString());
//        log.info("exec err: " + err.toString());
//        watch.close();
//    }
//
//    private static class SimpleListener implements ExecListener {
//
//        @Override
//        public void onOpen(Response response) {
//            System.out.println("The shell will remain open for 10 seconds.");
//        }
//
//        @Override
//        public void onFailure(Throwable t, Response response) {
//            System.err.println("shell barfed");
//        }
//
//        @Override
//        public void onClose(int code, String reason) {
//            System.out.println("The shell will now close.");
//        }
//    }
//
//    @Test
//    public void execLoopTest() throws InterruptedException {
//        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(20);
//        for (int i = 0; i < 10; System.out.println("i=" + i), i++) {
//            final CountDownLatch latch = new CountDownLatch(1);
//            ExecWatch watch = clientHolder.getClient().pods().inNamespace(DEFAULT_NAMESPACE).withName(pod.getMetadata().getName())
//                    // 读取输入，这种方式只能读取输入流，如果通过ExecWatch.getInput则为null
//                    .redirectingInput()
//                    .redirectingOutput()
//                    // 指定监听器，监听开始、失败、完成事件
//                    .usingListener(new ExecListener() {
//                        @Override
//                        public void onOpen(Response response) {
//                        }
//
//                        @Override
//                        public void onFailure(Throwable t, Response response) {
//                            latch.countDown();
//                        }
//
//                        @Override
//                        public void onClose(int code, String reason) {
//                            latch.countDown();
//                        }
//                    })
//                    // 执行指令
//                    .exec("date");
//            BlockingInputStreamPumper pump = new BlockingInputStreamPumper(watch.getOutput(), data -> log.info(new String(data)));
//            executorService.submit(pump);
//            Future<String> outPumpFuture = executorService.submit(pump, "Done");
//            executorService.scheduleAtFixedRate(new FutureChecker("Pump " + (i + 1), outPumpFuture), 0, 2, TimeUnit.SECONDS);
//
//            latch.await(5, TimeUnit.SECONDS);
//            log.info("exec out: ");
//            watch.close();
//
//        }
//    }
//
//    private static class FutureChecker implements Runnable {
//        private final String name;
//        private final Future<String> future;
//
//        private FutureChecker(String name, Future<String> future) {
//            this.name = name;
//            this.future = future;
//        }
//
//        @Override
//        public void run() {
//            if (!future.isDone()) {
//                System.out.println("Future:[" + name + "] is not done yet");
//            }
//        }
//    }
//
//    @Test
//    public void execPipesTest() {
//        ExecutorService executorService = Executors.newSingleThreadExecutor();
//        try (
//                ExecWatch watch = clientHolder.getClient().pods().inNamespace(DEFAULT_NAMESPACE).withName(pod.getMetadata().getName())
//                        // 读取输入，这种方式只能读取输入流，如果通过ExecWatch.getInput则为null
//                        .redirectingInput()
//                        .redirectingOutput()
//                        .redirectingError()
//                        .redirectingErrorChannel()
//                        // 执行指令
//                        .exec("sh");
//                BlockingInputStreamPumper pump = new BlockingInputStreamPumper(watch.getOutput(), data -> log.info(new String(data)))) {
//            executorService.submit(pump);
//            watch.getInput().write("ls -al\n".getBytes());
//            TimeUnit.SECONDS.sleep(5);
//        } catch (Exception e) {
//            throw KubernetesClientException.launderThrowable(e);
//        } finally {
//            executorService.shutdownNow();
//        }
//    }
//
//    @Test
//    public void execPodTest() {
//        String data = execCommandOnPod("ls", "-al");
//        log.info(data);
//    }
//
//    @SneakyThrows
//    public String execCommandOnPod(String... cmd) {
//        CompletableFuture<String> data = new CompletableFuture<>();
//        try (ExecWatch execWatch = execCmd(pod, data, cmd)) {
//            return data.get(10, TimeUnit.SECONDS);
//        }
//
//    }
//
//    private ExecWatch execCmd(Pod pod, CompletableFuture<String> data, String... command) {
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        return clientHolder.getClient().pods()
//                .inNamespace(pod.getMetadata().getNamespace())
//                .withName(pod.getMetadata().getName())
//                .writingOutput(baos)
//                .writingError(baos)
//                .usingListener(new Simple2Listener(data, baos))
//                .exec(command);
//    }
//
//    static class Simple2Listener implements ExecListener {
//
//        private CompletableFuture<String> data;
//        private ByteArrayOutputStream baos;
//
//        public Simple2Listener(CompletableFuture<String> data, ByteArrayOutputStream baos) {
//            this.data = data;
//            this.baos = baos;
//        }
//
//        @Override
//        public void onOpen(Response response) {
//            System.out.println("Reading data... " + response.message());
//        }
//
//        @Override
//        public void onFailure(Throwable t, Response response) {
//            System.err.println(t.getMessage() + " " + response.message());
//            data.completeExceptionally(t);
//        }
//
//        @Override
//        public void onClose(int code, String reason) {
//            System.out.println("Exit with: " + code + " and with reason: " + reason);
//            data.complete(baos.toString());
//        }
//    }
//}
