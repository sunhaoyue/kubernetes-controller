package com.kedacom.ctsp.iomp.k8s.common.cache;

import com.kedacom.ctsp.iomp.k8s.common.util.NamePreservingRunnable;
import io.fabric8.kubernetes.api.model.HasMetadata;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * @author keda
 */
@Slf4j
public class K8sCacheOperator implements K8sOperator {

    static final int MAX_TOTAL = 20;
    static final long DEFAULT_CACHE_TIME = 10 * 1000;

    private final K8sOperator k8sOperator;

    private final Executor executor;

    private final AtomicLong threadId = new AtomicLong(0);
    private String name = "cluster-cache-metadata";

    /**
     * 缓存最后一次更新时间
     */
    private AtomicLong lastTime = new AtomicLong(0);

    /**
     * 缓存刷新时机，刷新时间
     */
    private final long cacheTime = DEFAULT_CACHE_TIME;
    /**
     * 缓存时间(30分钟) + 10秒为数据有效时间
     */
    private final long invalidTime = DEFAULT_CACHE_TIME + 30 * 60000L;

    private final long requestTimeout = 5000L;

    private AtomicInteger count = new AtomicInteger(0);

    private List<? extends HasMetadata> data = new ArrayList<>();

    private AtomicReference<Processor> processorRef = new AtomicReference<>();

    private ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Queue<Thread> waitThread = new ConcurrentLinkedQueue<>();

    public K8sCacheOperator(K8sOperator k8sOperator, Executor executor, String name) {
        this.k8sOperator = k8sOperator;
        this.executor = executor;
        this.name = name;
//        startup();
    }


    @Override
    public List<? extends HasMetadata> list() {
        if (isRefresh()) {
            log.info("开始获取新的数据");
            startup(); // 缓存响应处理
        }
        return getDefaultData();
    }

    @Override
    public void refreshCache() {
        log.info("刷新所有数据");
        startup(); // 缓存响应处理
    }

    private List<? extends HasMetadata> getDefaultData() {

        if (isInvalid()) {
            Thread t = Thread.currentThread();
            // waitThread的增删注意要加锁；waitThread要注意刚加，
            // 然后已经结束；这里可以考虑单独加个锁， 在锁内再次判断 是否实效；
            try {
                waitThread.add(t);
                long total = 1;
                log.info("进入自旋等待..." + name);
                LockSupport.parkNanos(name, TimeUnit.MILLISECONDS.toNanos(1000));
                // 自旋
                while (isInvalid()) {
                    if (total++ > MAX_TOTAL) {
                        log.warn("等待超过10秒了..." + name);
                        return new ArrayList<>();
                    }
                    LockSupport.parkNanos(name, TimeUnit.MILLISECONDS.toNanos(1000));
                }
            } finally {
                log.info("离开自旋等待..." + name);
                waitThread.remove(t);
            }
        }

        try {
            if (lock.readLock().tryLock(requestTimeout, TimeUnit.MILLISECONDS)) {
                try {
                    return new ArrayList<>(data);
                } finally {
                    lock.readLock().unlock();
                }
            }
        } catch (InterruptedException e) {
            log.warn("thread InterruptedException", e);
        }
        return new ArrayList<>();
    }

    @Override
    public List<? extends HasMetadata> list(String namespace) {
        log.info("获取缓存列表=>namespace:{}", namespace);
        List<? extends HasMetadata> result = list();
        if (StringUtils.isEmpty(namespace)) {
            return result;
        }

        return result.stream()
                .filter(h -> {
                    String ns = h.getMetadata().getNamespace();
                    if (StringUtils.isBlank(ns)) {
                        // 如果为空，可能就没有空间这个字段， 如：pv
                        return true;
                    }
                    return StringUtils.equals(ns, namespace);
                }).collect(Collectors.toList());
    }

    @Override
    public <T extends HasMetadata> void patchCache(T metadata) {

    }

    @Override
    public void patchCache(List metadata) {
        log.info("更新缓存数据=>{}", metadata.size());
        try {
            lock.writeLock().lock();
            metadata.forEach(t -> {

                HasMetadata m = (HasMetadata) t;


                HasMetadata hasMetadata;
                if (StringUtils.isNotBlank(m.getMetadata().getNamespace())) {
                    hasMetadata = data.stream().filter(p -> p.getMetadata().getName().equals(m.getMetadata().getName()) && p.getMetadata().getNamespace().equals(m.getMetadata().getNamespace())).findFirst().orElse(null);
                } else {
                    hasMetadata = data.stream().filter(p -> p.getMetadata().getName().equals(m.getMetadata().getName())).findFirst().orElse(null);
                }
                if (hasMetadata != null) {
                    BeanUtils.copyProperties(m, hasMetadata);
                } else {
                    List tmp = new ArrayList();
                    tmp.add(t);
                    data.addAll(tmp);
                }
            });
        } finally {
            lock.writeLock().unlock();
        }

    }

    @Override
    public void delCache(String name) {

    }

    @Override
    public void delCacheByLabel(String namespace, String key, String... labels) {
        log.info("更新缓存数据=>namespace:{},key:{},labels", namespace, key, labels);
        try {
            lock.writeLock().lock();
            Iterator iterList = data.iterator();
            while (iterList.hasNext()) {
                HasMetadata tmp = (HasMetadata) iterList.next();
                if (StringUtils.isNotBlank(namespace)) {
                    if (!namespace.equals(tmp.getMetadata().getNamespace())) {
                        continue;
                    }
                }
                if (tmp.getMetadata().getLabels() != null && !tmp.getMetadata().getLabels().isEmpty()) {
                    String tmpStr = tmp.getMetadata().getLabels().get(key);
                    String tmpValue = Arrays.stream(labels).filter(t -> t.equals(tmpStr)).findFirst().orElse(null);
                    if (tmpValue != null) {
                        data.remove(tmp);
                        break;
                    }
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public <T extends HasMetadata> T toSimpleData(T metadata) {
        return null;
    }

    @Override
    public void delCache(String name, String namespace) {
        log.info("删除缓存数据=>name:{},namespace:{}", name, namespace);
        try {
            lock.writeLock().lock();
            if (CollectionUtils.isEmpty(data)) {
                return;
            }
            if (StringUtils.isNotBlank(namespace)) {
                data.removeIf(t -> t != null && t.getMetadata() != null && StringUtils.isNotBlank(t.getMetadata().getName()) && t.getMetadata().getName().equals(name) && StringUtils.isNotBlank(t.getMetadata().getNamespace()) && t.getMetadata().getNamespace().equals(namespace));
            } else {
                data.removeIf(t -> t != null && t.getMetadata() != null && StringUtils.isNotBlank(t.getMetadata().getName()) && t.getMetadata().getName().equals(name));
            }

        } finally {
            lock.writeLock().unlock();
        }
    }


    public void wakeup() {

    }

    public void wakeup(String namespace) {

    }

    public void wakeup(String namespace, String name) {

    }

    private boolean isRefresh() {
        log.info("cache time:" +  (System.currentTimeMillis() - lastTime.get()));
        return (System.currentTimeMillis() - lastTime.get() > cacheTime)  || (System.currentTimeMillis() - lastTime.get() < 0);
    }

    private boolean isTimeout(long timeout) {
        return System.currentTimeMillis() - lastTime.get() > timeout;
    }

    private boolean isInvalid() {
        return System.currentTimeMillis() - lastTime.get() > invalidTime;
    }

    private void startup() {
        Processor processor = processorRef.get();
        if (processor == null) {
            processor = new Processor();
            if (processorRef.compareAndSet(null, processor)) {
                executor.execute(new NamePreservingRunnable(processor, nextThreadName()));
            }
        }
    }

    private String nextThreadName() {
        return String.format("cache-%s-%s-%d", this.name, k8sOperator.getClass().getSimpleName(), threadId.incrementAndGet());
    }

    private void clear() {
        log.info("清理..." + k8sOperator.getClass().getSimpleName());
        processorRef.set(null);
        threadId.decrementAndGet();
        List<Thread> list = new ArrayList<>(waitThread);
        for (Thread thread : list) {
            LockSupport.unpark(thread);
        }
        log.info("完成..." + k8sOperator.getClass().getSimpleName());
    }

    private class Processor implements Runnable {
        @Override
        public void run() {
            List cache = new ArrayList();
            try {
                log.info("开始查询数据..." + k8sOperator.getClass().getSimpleName());
                List<? extends HasMetadata> temp = k8sOperator.list();
                for (HasMetadata metadata : temp) {
                    HasMetadata hasMetadata = k8sOperator.toSimpleData(metadata);
                    cache.add(hasMetadata);
                }
                log.info("数据查询结束..." + k8sOperator.getClass().getSimpleName());
            } catch (Exception e) {
                log.warn("缓存数据查询异常：" + k8sOperator.getClass().getSimpleName(), e);
                clear();
                return;
            }

            try {
                lock.writeLock().lock();
                data.clear();
                data.addAll(cache);
                lastTime.set(System.currentTimeMillis());
            } finally {
                lock.writeLock().unlock();
            }

            clear();

        }
    }


}
