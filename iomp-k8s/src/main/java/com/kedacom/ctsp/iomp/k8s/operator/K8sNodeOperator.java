package com.kedacom.ctsp.iomp.k8s.operator;


import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.kedacom.ctsp.iomp.k8s.K8sClientHolder;
import com.kedacom.ctsp.iomp.k8s.common.cache.K8sAbstractOperator;
import com.kedacom.ctsp.iomp.k8s.constant.K8sRegionConstant;
import com.kedacom.ctsp.iomp.k8s.vo.AffinityVo;
import io.fabric8.kubernetes.api.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * k8s node
 */
@Slf4j
public class K8sNodeOperator extends K8sAbstractOperator {

    /**
     * 极限单节点 换ip使用的一个annotation
     */
    private static String ANN_CHANGEIP = "dolphin/change-ip";

    public K8sNodeOperator(K8sClientHolder k8sClientHolder) {
        super(k8sClientHolder);
    }


    /**
     * 给主机节点添加标签
     *
     * @param nodeName
     * @param labelsMap
     */
    public Node addNodeLabels(String nodeName, Map<String, String> labelsMap) {
        log.info("get => nodeName:{},labelsMap:{}", nodeName, labelsMap);
        Node node = k8sClientHolder.getClient().nodes().withName(nodeName).edit(n -> new NodeBuilder(n).editMetadata().addToLabels(labelsMap).endMetadata().build());
        this.patchCache(node);
        return node;
    }

    /**
     * 移除主机节点标签
     *
     * @param nodeName
     * @param labelsMap
     */
    public void delNodeLabels(String nodeName, Map<String, String> labelsMap) {
        log.info("get => nodeName:{},labelsMap:{}", nodeName, labelsMap);
        Node node = k8sClientHolder.getClient().nodes().withName(nodeName).get();
        for (String key : labelsMap.keySet()) {
            node.getMetadata().getLabels().remove(key);
        }
        if (log.isDebugEnabled()) {
            log.debug("node:{}", JSON.toJSONString(node));
        }
        k8sClientHolder.getClient().nodes().withName(nodeName).patch(node);
        this.patchCache(node);
    }


    public boolean delete(String name) {
        log.info("delete => name:{}", name);
        boolean result = k8sClientHolder.getClient().nodes().withName(name).delete();
        if (result) {
            this.delCache(name);
        }
        return result;
    }


    // TODO: 2021/12/28 特殊
    public List<Node> getWithLabel(String key) {
        log.info("get => key:{}", key);
        return k8sClientHolder.getClient().nodes().withLabel(key).list().getItems();
    }


    public List<Node> getWithoutLabel(String key) {
        log.info("get => key:{}", key);
        return k8sClientHolder.getClient().nodes().withoutLabel(key).list().getItems();
    }

    public List<LabelSelectorRequirement> getLabelSelector(List<AffinityVo> labels) {
        log.info("get => labels:{}", JSON.toJSONString(labels));
        return labels.stream().map(label ->
                new LabelSelectorRequirement(label.getKey(), label.getOperator(),
                        Optional.ofNullable(label.getValues()).map(e -> e.split(","))
                                .map(Arrays::asList).orElseGet(ArrayList::new))
        ).collect(Collectors.toList());
    }

    public List<Node> withAffinityAndRegionLabel(List<AffinityVo> labels, String regionSign) {
        log.info("get => labels:{},regionSign{}", JSON.toJSONString(labels), regionSign);
        List<LabelSelectorRequirement> labelSelectors = getLabelSelector(labels);
        handleLabelSelector(labelSelectors);
        ArrayList<Node> nodes = Lists.newArrayList();
        List<Node> commonNodes = k8sClientHolder.getClient().nodes().withLabelSelector(new LabelSelectorBuilder()
                .withMatchExpressions(labelSelectors).build()).withLabelIn(K8sNamespaceOperator.LABEL_REGION, regionSign)
                .list().getItems();
        if (CollectionUtils.isNotEmpty(commonNodes)) {
            nodes.addAll(commonNodes);
        }
        if (Objects.equals(regionSign, K8sRegionConstant.COMMON_TYPE)) {
            List<Node> withLabelNodes = withoutAffinityAndRegionLabel(labels);
            if (CollectionUtils.isNotEmpty(withLabelNodes)) {
                nodes.addAll(withLabelNodes);
            }
        }
        return nodes;
    }

    public List<Node> withoutAffinityAndRegionLabel(List<AffinityVo> labels) {
        log.info("get => labels:{}", JSON.toJSONString(labels));
        List<LabelSelectorRequirement> labelSelectors = getLabelSelector(labels);
        handleLabelSelector(labelSelectors);
        return k8sClientHolder.getClient().nodes().withLabelSelector(new LabelSelectorBuilder()
                .withMatchExpressions(labelSelectors).build())
//                .withoutLabel(K8sNamespaceOperator.LABEL_REGION)
                .list().getItems();
    }

    private void handleLabelSelector(List<LabelSelectorRequirement> labelSelectors) {
        for (int i = 0; i < labelSelectors.size(); i++) {
            String operator = labelSelectors.get(i).getOperator();
            // kubernetes labelSelector 只支持以下几种表达式
            if (StringUtils.equalsAny(operator, "In", "NotIn", "DoesNotExist", "Exists")) {
                continue;
            }
            labelSelectors.remove(i);
            i--;
        }
    }


    public List<Node> getWithLabelExistAndMatchLabels(List<String> existLabels, Map<String, String> labels) {
        log.info("get => existLabels:{},labels{}", JSON.toJSONString(existLabels), JSON.toJSONString(labels));
        List<LabelSelectorRequirement> requirements = Lists.newArrayList();
        for (String label : existLabels) {
            requirements.add(new LabelSelectorRequirementBuilder()
                    .withKey(label).withOperator("Exists").build());
        }
        if (labels != null && labels.size() > 0) {
            Iterator<Map.Entry<String, String>> iterator = labels.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> next = iterator.next();
                requirements.add(new LabelSelectorRequirementBuilder()
                        .withKey(next.getKey()).withOperator("In").withValues(next.getValue()).build());
                // 如果默认域为common,则将域标签为空的也筛选出来
//                if (Objects.equals(K8sRegionConstant.COMMON_TYPE, next.getValue())) {
//                    requirements.add(new LabelSelectorRequirementBuilder()
//                            .withKey(next.getKey()).withOperator("In").withValues("").build());
//                }
            }
        }
        List<Node> nodes = k8sClientHolder.getClient().nodes().withLabelSelector(new LabelSelectorBuilder().withMatchExpressions(requirements).build()).list().getItems();
        if (StringUtils.isNotEmpty(labels.get("dolphin.region/name"))) {
            nodes.addAll(k8sClientHolder.getClient().nodes().withoutLabel("dolphin.region/name").list().getItems());
        }
        return nodes;
    }

    public void createOrReplace(Node... nodes) {
        log.info("update => nodes:{}", nodes);
        k8sClientHolder.getClient().nodes().createOrReplace(nodes);
        this.patchCache(Arrays.asList(nodes));
    }

    public void createOrReplace(List<Node> nodes) {
        log.info("update => nodes:{}", JSON.toJSONString(nodes));
        for (Node node : nodes) {
            k8sClientHolder.getClient().nodes().createOrReplace(node);
            this.patchCache(node);
        }
    }

    /**
     * 根据域来查询集群主机
     * 1.按照域标签来查询主机
     * 2.如果是默认主机，那么没有域标签的主机也算是默认域的主机
     *
     * @param regionSign
     * @return
     */
    public List<Node> getWithRegion(String regionSign) {
        log.info("get => regionSign:{}", regionSign);
        List<Node> nodes = listByLabelCustom("dolphin.region/name", regionSign);
        if (StringUtils.equals(K8sRegionConstant.COMMON_TYPE, regionSign)) {
            nodes.addAll(getWithoutLabel("dolphin.region/name"));
        }
        return nodes;
    }

    //------------------------------------------------------------------------以下为重构方法--------------------------------/
    public List<Node> list() {
        log.info("list all");
        return k8sClientHolder.getClient().nodes().list().getItems();

    }

    @Override
    public List<Node> list(String namespace) {
        return this.list();
    }

    public List<Node> list(Long limit) {

        // 若值为空或小于0则默认100条
        if (limit == null || limit < 0) {
            return this.list();
        }
        log.info("get => limit:{}", limit);
        return k8sClientHolder.getClient().nodes().list(new ListOptionsBuilder().withLimit(limit).build()).getItems();
    }

    /**
     * 根据名称获取对象
     *
     * @param name
     * @return
     */
    public Node get(String name) {
        log.info("get => name:{}", name);
        return k8sClientHolder.getClient().nodes().withName(name).get();
    }

    public List<Node> listByLabels(Map<String, String> labels) {
        log.info("get => labels:{}", labels);
        return k8sClientHolder.getClient().nodes().withLabels(labels).list().getItems();
    }

    public List<Node> listByLabels(List<String> labels) {
        log.info("get => labels:{}", JSON.toJSONString(labels));
        List<LabelSelectorRequirement> requirements = Lists.newArrayList();
        for (String label : labels) {
            requirements.add(new LabelSelectorRequirementBuilder()
                    .withKey(label).withOperator("Exists").build());
        }
        return k8sClientHolder.getClient().nodes().withLabelSelector(new LabelSelectorBuilder().withMatchExpressions(requirements).build()).list().getItems();
    }

    public List<Node> listByLabelCustom(String label, String... value) {
        log.info("get => label:{},value{}", label, value);
        return k8sClientHolder.getClient().nodes().withLabelIn(label, value).list().getItems();
    }

}
