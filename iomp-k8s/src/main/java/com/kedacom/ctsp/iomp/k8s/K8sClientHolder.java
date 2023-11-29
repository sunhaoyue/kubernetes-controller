package com.kedacom.ctsp.iomp.k8s;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.kedacom.ctsp.iomp.k8s.common.cache.K8sCacheOperator;
import com.kedacom.ctsp.iomp.k8s.common.cache.K8sOperator;
import com.kedacom.ctsp.lang.exception.CommonException;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * docker client holder
 *
 * @author hefei
 * @create 2018-01-11 上午10:31
 **/
@Slf4j
public class K8sClientHolder {

    private static final String HTTP_PROTOCOL = "http://";
    private static final String HTTPS_PROTOCOL = "https://";
    private final Map<String, DefaultKubernetesClient> clients = new ConcurrentHashMap<>();


    private ExecutorService executor;
    private Map<Class<?>, K8sOperator> cacheMap = new ConcurrentHashMap<>();
    private final Object lock = new Object();

    @Getter
    private final K8sConfig k8sConfig;

    public K8sClientHolder(DefaultKubernetesClient client, K8sConfig k8sConfig) {
        this.k8sConfig = k8sConfig;
        List<String> masters = k8sConfig.getMasters();
        for (String master : masters) {
            clients.put(master, client);
        }
    }

    public K8sClientHolder(K8sConfig k8sConfig) {
        this.k8sConfig = k8sConfig;
        initClient();
    }

    public boolean useCache() {
        return executor != null;
    }

    public K8sOperator getCacheOperator(Class<?> clazz) {
        K8sOperator k8sResourceCache = cacheMap.get(clazz);
        if (k8sResourceCache != null) {
            return k8sResourceCache;
        }
        throw new CommonException("iomp.k8s.cacheclass.notavailable",clazz.getSimpleName());
        // 下边可以通过switch 生成响应的处理方法；
    }

    public void put(K8sOperator operator) {
        Class<?> clazz = operator.getClass();
        K8sOperator k8sCacheOperator = cacheMap.get(clazz);
        if (k8sCacheOperator != null) {
            return;
        }
        synchronized (lock) {
            k8sCacheOperator = cacheMap.get(clazz);
            if (k8sCacheOperator != null) {
                return;
            }
            if (executor != null) {
                cacheMap.put(clazz, new K8sCacheOperator(operator, executor, k8sConfig.getK8sName()));
            } else {
                cacheMap.put(clazz, operator);
            }
        }
    }

    public void initCache() {
        // 增加最大线程池，保证统计数量接口不报错
        executor = new ThreadPoolExecutor(2, 18,
                0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(), new ThreadFactoryBuilder().setNameFormat("cache-cluster-%d").build());
    }

    public void destroy() {
        if (executor != null) {
            try {
                executor.shutdownNow();
                executor.awaitTermination(1, TimeUnit.SECONDS);
                if (!executor.isTerminated()) {
                    log.warn("cluster executor has not terminated. , warning...");
                }
            } catch (InterruptedException e) {
                // ignore
            }
        }
        synchronized (lock) {
            cacheMap.clear();
            // 这里加一个状态， 如果destory，那么就不能再使用了。
        }
    }

    /**
     * 获取k8s client
     * <p>
     * 此方法根据配置的网络类型返回不同的docker client
     *
     * @return
     * @throws K8sUnavailableException
     */
    public DefaultKubernetesClient getClient() {
        DefaultKubernetesClient client = getActiveClient();
        if (client != null) {
            return client;
        }
        log.warn("not get k8s api server client, reset client,");
        // 重新初始化一下客户端，url或者证书会有缓存
        initClient();
        log.warn("reset client complete, try get client again");
        // 如果没获取到，那么再尝试一次
        client = getActiveClient();
        if (client == null) {
//            throw new K8sUnavailableException("K8s所有的api server 无法连接。");
            throw new K8sCannotConnectException("iomp.k8s.api.error", k8sConfig.getK8sName());
        }
        return client;
    }

    public DefaultKubernetesClient getActiveClient() {
        List<String> masters = new ArrayList<>(k8sConfig.getMasters());
        for (String k8sManager : masters) {
            DefaultKubernetesClient client = clients.get(k8sManager);
            if (ping(client)) {
                return client;
            }
            log.warn("cluster api Server[{}] request failed！try next", k8sManager);
            // 把这个manager放到最后
            rotate(k8sManager);
        }
        return null;
    }

    public String getActiveMaster() {
        for (String k8sManager : k8sConfig.getMasters()) {
            DefaultKubernetesClient client = clients.get(k8sManager);
            if (ping(client)) {
                return k8sManager;
            }
            log.warn("cluster api Server[{}] request failed！try next", k8sManager);
        }

        return null;
    }

    public String getHttpsActiveServer() {
        for (String k8sManager : k8sConfig.getMasters()) {
            DefaultKubernetesClient client = clients.get(k8sManager);
            if (ping(client)) {
                return String.format("https://%s", k8sManager);
            }
            log.warn("cluster api Server[{}] request failed！try next", k8sManager);
        }

        return null;
    }

    private void rotate(String master) {
        if (k8sConfig.getMasters().size() <= 1) {
            return;
        }
        List<String> list = new ArrayList<>();
        k8sConfig.getMasters().forEach(m -> {
            if (!StringUtils.equals(m, master)) {
                list.add(m);
            }
        });
        list.add(master);
        k8sConfig.setMasters(list);
    }

    /**
     * 初始化swarm client
     * <p>
     * 在集群创建成功后进行初始化
     */
    private void initClient() {
        RuntimeException exception = null;
        clients.clear();
        for (String k8sManager : k8sConfig.getMasters()) {
            DefaultKubernetesClient client = null;
            try {
                client = generateClient(k8sManager);
            } catch (RuntimeException e) {
                log.error(String.format("k8s master %s, 连接失败", k8sManager), e);
                exception = e;
            }
            if (client != null) {
                log.info("生成的kubernetes客户端，k8sManager={}", k8sManager);
                clients.put(k8sManager, client);
            }
        }
        if (clients.size() == 0) {
            if (exception == null) {
                exception = new K8sUnavailableException("没有可用的k8s master节点。");
            }
            throw exception;
        }
    }

    /**
     * 检查docker client 状态
     *
     * @param client
     * @return
     */
    private boolean ping(DefaultKubernetesClient client) {
        try {
            if (client.getVersion() == null) {
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("客户端异常", e);
            return false;
        }
    }

    private DefaultKubernetesClient generateClient(String k8sMaster) {
        Config config;
        if (null == k8sConfig.getProtocol() || Net7Protocol.HTTP.name().equals(k8sConfig.getProtocol().toUpperCase())) {
            //http 协议
            config = new ConfigBuilder().withMasterUrl(String.format("%s%s", HTTP_PROTOCOL, k8sMaster)).withDisableHostnameVerification(true).build();
        } else {
            //https 协议
            log.info("caCertFile: {}, clientCertFile: {}, clientKeyFile: {}",
                    k8sConfig.getCaCertFile(), k8sConfig.getClientCertFile(), k8sConfig.getClientKeyFile());
            String ca = "";
            String cacrt = "";
            String cakey = "";
            try {
                ca = FileUtils.readFileToString(new File(k8sConfig.getCaCertFile()), "utf-8");
                cacrt = FileUtils.readFileToString(new File(k8sConfig.getClientCertFile()), "utf-8");
                cakey = FileUtils.readFileToString(new File(k8sConfig.getClientKeyFile()), "utf-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
            config = new ConfigBuilder().withMasterUrl(String.format("%s%s", HTTPS_PROTOCOL, k8sMaster))
                    .withDisableHostnameVerification(true)
                    .withRequestTimeout(30000)
                    .withConnectionTimeout(10000)
                    .withMaxConcurrentRequestsPerHost(100)
                    .withCaCertData(Base64.getEncoder().encodeToString(ca.getBytes()))
                    .withClientCertData(Base64.getEncoder().encodeToString(cacrt.getBytes()))
                    .withClientKeyData(Base64.getEncoder().encodeToString(cakey.getBytes()))
                    .withMaxConcurrentRequestsPerHost(100)
                    .withWebsocketTimeout(60000)
                    .build();
        }
        return new DefaultKubernetesClient(config);
    }


    /*    */

}
