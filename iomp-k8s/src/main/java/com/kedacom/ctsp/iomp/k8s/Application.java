package com.kedacom.ctsp.iomp.k8s;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.kedacom.ctsp.iomp.k8s.Entity.*;
import com.kedacom.ctsp.iomp.k8s.common.util.IPUtils;
import com.kedacom.ctsp.iomp.k8s.common.util.MySQLUtil;
import com.kedacom.ctsp.iomp.k8s.common.util.SqliteUtil;
import com.kedacom.ctsp.iomp.k8s.common.util.YamlUtil;
import com.kedacom.ctsp.iomp.k8s.common.util.shell.CmdExecUtil;
import com.kedacom.ctsp.iomp.k8s.constant.K8sKindConstant;
import com.kedacom.ctsp.iomp.k8s.operator.K8sConfigmapOperator;
import com.kedacom.ctsp.lang.mapper.BeanMapper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.kedacom.ctsp.iomp.k8s.common.util.MySQLUtil.*;
import static com.kedacom.ctsp.iomp.k8s.common.util.SqliteUtil.DB_URL_PREFIX;
import static com.kedacom.ctsp.iomp.k8s.common.util.SqliteUtil.PRE_DB;
import static com.kedacom.ctsp.iomp.k8s.constant.K8sConstants.CLUSTER_VERSION_CONFIGMAP;

/**
 * @author sunhaoyue
 * @date Created in 2023/11/7 18:50
 */

@Slf4j
public class Application {
    String DEFAULT = "default";
    static String CLUSTER_VERSION_KEY = "cluster-version";
    String CLUSTER_VERSION_DEFAULT = "v2.5.1";

    public Integer defaultSecurePort = 6443;

    String HARDCORE_GATEWAY_IP = "192.177.1.100";
    String HTTPS = "https";
    static String DATA_NAME = "iomp";
    /**
     * 极限模式
     */
    static String EXTREME = "extreme";
    /**
     * 极限模式
     */
    static String HARDCORE = "hardcore";

    static String kubeRoot = "/data/iomp_data/kube/";
    /**
     * 路径: /data/iomp_data/cert/7becb15a122b4b07b52a26ad8ca8b1a8/
     */
    static String certRoot = "/data/iomp_data/cert/";

    /**
     * sshkey 路径
     */
    static String sshKey = "/data/iomp_data/ssh_key/";

    //Main-Class: com.kedacom.ctsp.iomp.k8s.Application
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        String directoryPath = "/data/dolphin/"; // 替换为要保存文件的目录路径
        String configFilePath = "/data/clean/"; // 替换为了要保存数据源文件的目录路径
        createDirectory(directoryPath);
        createDirectory(configFilePath);
        String sqlClusterAll = "select * from n_cluster ";
        String sqlNodeAll = "select * from n_node ";

        UpgradeDBVO dbvo = new UpgradeDBVO();
        UpgradeDBSource dbSource = new UpgradeDBSource();

        ClusterAndNode clusterAndNode = new ClusterAndNode();
        List<ClusterVO> clusterInfo = new ArrayList<>();
        List<ClusterEntity> clusterEntities;
        List<NodeEntity> nodeEntities;

        if (args.length > 0) {
            String url = args[0];
            String port = (args.length >= 2) ? args[1] : "3306";
            clusterEntities = MySQLUtil.executeQuery(url, port, sqlClusterAll, ClusterEntity.class);
            nodeEntities = MySQLUtil.executeQuery(url, port, sqlNodeAll, NodeEntity.class);

            dbSource.setDbType("mysql");
            dbSource.setUrl(String.format(DB_URL, url, port));
            dbSource.setUsername(DB_USERNAME);
            dbSource.setPassword(DB_PASSWORD);
        } else {
            dbSource.setDbType("sqlite");
            dbSource.setUrl(DB_URL_PREFIX + DATA_NAME + PRE_DB);
            clusterEntities = SqliteUtil.executeQuery(DATA_NAME, sqlClusterAll, ClusterEntity.class);
            nodeEntities = SqliteUtil.executeQuery(DATA_NAME, sqlNodeAll, NodeEntity.class);
        }

        dbvo.setDbSource(dbSource);
        String config = YamlUtil.convertObjToYaml1(dbvo);
        String saveConfigFilePath = configFilePath + "264.yaml";
        downloadUsingStream(config, saveConfigFilePath);
        System.out.println("保存的升级配置数据源文件路径:" + saveConfigFilePath);

        for (ClusterEntity entity : clusterEntities) {
            // 不是运行中的主机 24x部署这样的：
           /* if (!NumberUtil.equals(entity.getStatus(), 6)) {
                continue;
            }*/
            String clusterId = entity.getId();
            List<NodeEntity> clusterNodeEntities = nodeEntities.stream()
                    .filter(node -> StringUtils.isNotEmpty(node.getClusterId()) && node.getClusterId().equals(clusterId))
                    .collect(Collectors.toList());

            ClusterVO clusterVO = new ClusterVO();
            BeanMapper.copy(entity, clusterVO);
            clusterVO.setName(entity.getK8sName());
            clusterVO.setService_subnet(StringUtils.isNotEmpty(entity.getServiceSubnet()) ? entity.getServiceSubnet() : "10.96.0.0/12");

            //  kubectl version --short|grep 'Server' |awk '{print $3}'
            String command = "kubectl --kubeconfig=" + getClusterPath(clusterId) + " version --short |grep 'Server' |awk '{print $3}' ";
            String k8sVersion = CmdExecUtil.execGetInfo(command);
            /**
             *
             *     Client Version: v1.21.6+k3s-
             *     Server Version: v1.21.8
             */
            int startIndex = k8sVersion.indexOf("Server Version: ") + "Server Version: ".length();
            int endIndex = k8sVersion.indexOf("\n", startIndex);
            String version = k8sVersion.substring(startIndex, endIndex);
            clusterVO.setK8sVersion(version);


            clusterVO.setScript_type(StringUtils.equalsIgnoreCase(entity.getScriptType(), HARDCORE) ? "hardcore" : "");
            clusterVO.setKubeRoot(kubeRoot + clusterId + "/");
            clusterVO.setCertRoot(certRoot + clusterId + "/");
            int masterCount = clusterNodeEntities.stream().filter(node -> Objects.equals(node.getIsMeta(), 1L)).collect(Collectors.toList()).size();
            clusterVO.setMasterCount(masterCount);
            clusterVO.setSshKey(StringUtils.isNotEmpty(entity.getWorkMode()) ? sshKey : "");
            String coredns_type = "mult-coredns";
            if (StringUtils.equalsIgnoreCase(entity.getScriptType(), HARDCORE) || masterCount == 1) {
                coredns_type = "only-coredns";
            }
            clusterVO.setCoredns_type(coredns_type);
            clusterVO.setRegistry_url(entity.getRegistryUrl());
            clusterVO.setRegistry_project(entity.getRegistryEnv());
            List<NodeVO> nodes = clusterNodeEntities.stream().map(Application::convertFrom).collect(Collectors.toList());
            clusterVO.setNodes(nodes);
            if (CollectionUtils.isEmpty(clusterVO.getNodes())) {
                continue;
            }
            clusterInfo.add(clusterVO);
        }

        clusterAndNode.setClusters(clusterInfo);
        String content = YamlUtil.convertObjToYaml1(clusterAndNode);
        String saveFilePath = directoryPath + "cluster-info.yaml";
        downloadUsingStream(content, saveFilePath);
        System.out.println("保存的文件路径:" + saveFilePath);
    }

    public static String getClusterPath(String clusterId) {
        return MessageFormat.format("/data/iomp_data/kube/{0}/root/.kube/config", clusterId);
    }

    public static NodeVO convertFrom(NodeEntity source) throws IllegalFormatException {
        NodeVO node = new NodeVO();
        BeanMapper.copy(source, node);
        node.setNodename(StringUtils.isEmpty(source.getK8sNodeName()) ? "k8s-" + source.getHostIp() : source.getK8sNodeName());
        if (StringUtils.isEmpty(source.getPassword())) {
            node.setSshAuthType("key");
        }
        node.setRole(Objects.equals(source.getIsMeta(), 1L) ? "master" : "worker");
        return node;
    }


    /**
     * 创建文件
     *
     * @param directoryPath
     */
    public static void createDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        directory.mkdirs();

    }

    public static void downloadUsingStream(String content, String saveFilePath) {
        File file = new File(saveFilePath);
        try {
            boolean fileCreated = file.createNewFile();
            if (fileCreated) {
                System.out.println("文件创建成功！");
            } else {
                System.out.println("文件已存在，无需创建。");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (OutputStream outputStream = new FileOutputStream(file)) {
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            outputStream.write(contentBytes);
            System.out.println("集群信息已成功保存到文件！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private K8sClientHolder createK8sClientHolder(String clusterId, ClusterEntity entity, List<NodeEntity> nodeEntities) {
        List<String> gatewayIps = this.queryMasters(nodeEntities);
        List<String> masters = gatewayIps.stream().map(master -> IPUtils.getK8sMaster(master) + ":" + defaultSecurePort)
                .collect(Collectors.toList());
        K8sConfig config = new K8sConfig();
        config.setK8sName(StringUtils.isBlank(entity.getK8sSign()) ? entity.getK8sName() : entity.getK8sSign());
        config.setProtocol(HTTPS);
        if ((isExtremeMode(entity.getClusterType(), entity.getScriptType()))
                && nodeEntities.size() == 1) {
            config.setMasters(Lists.newArrayList(HARDCORE_GATEWAY_IP + ":" + (defaultSecurePort)));
        } else {
            config.setMasters(masters);
        }
        //   /data/iomp_data/kube/f9386e54027d48e7ad6bcb8fc3e5c1a0/etc/kubernetes/pki/ca.crt
        config.setCaCertFile(entity.getCaCertFile());
        config.setClientCertFile(entity.getClientCertFile());
        config.setClientKeyFile(entity.getClientKeyFile());
        log.info("create k8sHolder, clusterId={}, config={}", clusterId, JSON.toJSONString(config));
        return new K8sClientHolder(config);
    }

    /**
     * 是否为极限模式
     *
     * @return
     */
    public static boolean isExtremeMode(String clusterType, String scriptType) {
        return StringUtils.equalsIgnoreCase(clusterType, EXTREME)
                || StringUtils.equalsIgnoreCase(scriptType, HARDCORE);
    }

    public List<String> queryMasters(List<NodeEntity> entities) {
        if (CollectionUtils.isEmpty(entities)) {
            return Lists.newArrayList();
//            throw new ClusterNotCompleteException("集群中主机信息为空");
        }
        List<String> masters = new ArrayList<>();
        entities.forEach(et -> {
            if (Objects.equals(et.getIsMeta(), 1)) {
                masters.add(et.getHostIp());
            }
        });
        return masters;
    }


    public Map<String, String> getClusterInfo(K8sClientHolder clientHolder) {
        K8sConfigmapOperator configmapOperator = new K8sConfigmapOperator(clientHolder);
        ConfigMap configmap = configmapOperator.get(CLUSTER_VERSION_CONFIGMAP, DEFAULT);
        if (configmap != null && !StringUtils.equalsIgnoreCase(configmap.getKind(), K8sKindConstant.CM)) {
            configmapOperator.deleteConfigMap(CLUSTER_VERSION_CONFIGMAP, DEFAULT);
            configmap = null;
        }
        Map<String, String> result = Maps.newHashMap();
        //集群上没有值，都走默认值
        if (configmap == null || configmap.getData() == null) {
            result.put(CLUSTER_VERSION_KEY, CLUSTER_VERSION_DEFAULT);
            return result;
        }
        Map<String, String> data = configmap.getData();
        result.put(CLUSTER_VERSION_KEY, data.get(CLUSTER_VERSION_KEY));

        return result;
    }


}
