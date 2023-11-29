package com.kedacom.ctsp.iomp.k8s.constant;

/**
 * @author tangjixing
 * @date 2020/1/16
 */
public interface K8sLabels {

    /**
     * 标签， 注意，这里的单词 拼写 是错误的，但我们用的就是这个值
     */
    String CATEGORY = "catagory";

    /**
     * pvc的来源， 没有就是来自云存储， 有值为： localpv
     */
    String LABEL_STORAGE_SOURCE = "dolphin/storage-source";
}
