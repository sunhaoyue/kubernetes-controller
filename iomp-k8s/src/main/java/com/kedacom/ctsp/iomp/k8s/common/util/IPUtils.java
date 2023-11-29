package com.kedacom.ctsp.iomp.k8s.common.util;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeAddress;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Slf4j
public class IPUtils {

    /**
     * 极限单节点 换ip使用的一个annotation
     */
    private static String ANN_CHANGEIP = "dolphin/change-ip";

    private static Pattern VALID_IPV4_PATTERN = null;
    private static Pattern VALID_IPV6_PATTERN = null;
    public static final String ipv4Pattern = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";
    public static final String ipv6Pattern = "^\\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:)))(%.+)?\\s*$";

    static {
        try {
            VALID_IPV4_PATTERN = Pattern.compile(ipv4Pattern, Pattern.CASE_INSENSITIVE);
            VALID_IPV6_PATTERN = Pattern.compile(ipv6Pattern, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            log.error("Unable to compile ipv4address or ipv6address pattern.", e);
        }
    }

    /**
     * 判断是否是ipv4或ipv6的ip
     *
     * @param ipAddress
     * @return
     */
    public static boolean isIpAddress(String ipAddress) {
        return isIpv4Address(ipAddress) || isIpv6Address(ipAddress);
    }

    public static boolean isIpv4Address(String ipAddress) {
        if (StringUtils.isBlank(ipAddress)) {
            return false;
        }
        Matcher ip4Matcher = VALID_IPV4_PATTERN.matcher(ipAddress);
        return ip4Matcher.matches();
    }

    public static boolean isIpv6Address(String ipAddress) {
        if (StringUtils.isBlank(ipAddress)) {
            return false;
        }
        Matcher ip6Matcher = VALID_IPV6_PATTERN.matcher(ipAddress);
        return ip6Matcher.matches();
    }

    /**
     * fullIP to abbreviationIP
     * fd00:0:0:0:0:0:0:11 -> fd00::11
     * 127.0.0.1 -> 127.0.0.1
     * fd00::11 -> fd00::11
     *
     * @param fullIP
     * @return
     */
    public static String parseFullIPToAbbreviation(String fullIP) {
        String abbreviation = "";
        // 1,校验 ":" 的个数 不等于7  或者长度不等于39  直接返回空串
        int ipv4Count = fullIP.length() - fullIP.replaceAll("\\.", "").length();
        //ipv4的直接返回ipv4地址
        if (ipv4Count == 3) {
            return fullIP;
        }
        int ipv6Count = fullIP.length() - fullIP.replaceAll(":", "").length();
        if (ipv6Count == 2) {
            return fullIP;
        }
        if (ipv6Count != 7) {
            return abbreviation;
        }

        // 2,去掉每一位前面的0
        String[] arr = fullIP.split(":");

        for (int i = 0; i < arr.length; i++) {
            arr[i] = arr[i].replaceAll("^0{1,3}", "");
        }

        // 3,找到最长的连续的0
        String[] arr2 = arr.clone();
        for (int i = 0; i < arr2.length; i++) {
            if (!"0".equals(arr2[i])) {
                arr2[i] = "-";
            }
        }

        Pattern pattern = Pattern.compile("0{2,}");
        Matcher matcher = pattern.matcher(StringUtils.join(arr2, ""));
        String maxStr = "";
        int start = -1;
        int end = -1;
        while (matcher.find()) {
            if (maxStr.length() < matcher.group().length()) {
                maxStr = matcher.group();
                start = matcher.start();
                end = matcher.end();
            }
        }

        // 3,合并        
        if (maxStr.length() > 0) {
            for (int i = start; i < end; i++) {
                arr[i] = ":";
            }
        }
        abbreviation = StringUtils.join(arr, ":");
        abbreviation = abbreviation.replaceAll(":{2,}", "::");

        return abbreviation;
    }


    /**
     * 根据hostname 获取fullIP
     * fd00::11 ->fd00:0:0:0:0:0:0:11
     * 127.0.0.1 ->127.0.0.1
     *
     * @param hostName
     * @return
     */
    public static String getFullIP(String hostName) {
        String fullIP = hostName;
        try {
            InetAddress inetAddr = InetAddress.getByName(hostName);
            fullIP = inetAddr.getHostAddress();
        } catch (Exception e) {
            log.error("get fullIP by hostName,", e);
        }
        return fullIP;
    }

    /**
     * 去除中括号
     * [fd00:0:0:0:0:0:0:11] -> fd00:0:0:0:0:0:0:11
     *
     * @param ip
     * @return
     */
    public static String removeBrackets(String ip) {
        if (StringUtils.isNotBlank(ip)) {
            ip = ip.replace("[", "").replace("]", "");
        }
        return ip;
    }

    /**
     * 添加中括号
     * fd00:0:0:0:0:0:0:11 -> [fd00:0:0:0:0:0:0:11]
     *
     * @param ip
     * @return
     */
    public static String appendBrackets(String ip) {
        if (StringUtils.isNotBlank(ip)) {
            ip = "[" + ip + "]";
        }
        return ip;
    }

    /**
     * 根据ip类型返回k8s的master的格式
     * 127.0.0.1 -> 127.0.0.1
     * fd00:0:0:0:0:0:0:11 -> [fd00:0:0:0:0:0:0:11]
     *
     * @param ip
     * @return
     */
    public static String getK8sMaster(String ip) {
        if (StringUtils.isNotBlank(ip)) {
            int ipv4Count = ip.length() - ip.replaceAll("\\.", "").length();
            //ipv4的直接返回ipv4地址
            if (ipv4Count == 3) {
                return ip;
            }
            int ipv6Count = ip.length() - ip.replaceAll(":", "").length();
            if (ipv6Count == 7 || ipv6Count == 2) {
                return appendBrackets(ip);
            }
        }
        return ip;
    }

    /**
     * fullIp to abbreviationIP  no colon
     * fd00:0:0:0:0:0:0:11  ->  fd0011
     *
     * @param ip
     * @return
     */
    public static String fullIPToNoColon(String ip) {
        if (StringUtils.isNotBlank(ip)) {
            ip = StringUtils.remove(parseFullIPToAbbreviation(ip), "::");
        }
        return ip;
    }

    /**
     * 验证ip是否Reachable
     * 验证连接超时时间3s
     *
     * @param ip
     * @return
     */
    public static boolean isReachable(String ip) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            return inetAddress.isReachable(3000);
        } catch (Exception e) {
            log.error("jugde ip isReachable error ip= {} ", ip, e);
        }
        return false;
    }


    /**
     * 先从node的matadata上拿，为了兼容极限集群单节点换ip的场景，因为换ip不替换node的internalIp字段
     * 再从node上的internalIp上拿
     *
     * @param node
     * @return
     */
    public static String getInternalIp(Node node) {
        Map<String, String> annotations = node.getMetadata().getAnnotations();
        if (annotations != null && StringUtils.isNotEmpty(annotations.get(ANN_CHANGEIP))) {
            return annotations.get(ANN_CHANGEIP);
        } else {
            return IPUtils.getNodeInternalIp(node.getStatus().getAddresses());
        }
    }

    /**
     * 获取node internal ip
     *
     * @param nodeAddressList
     * @return
     */
    public static String getNodeInternalIp(List<NodeAddress> nodeAddressList) {
        String internalIp = null;
        for (NodeAddress address : nodeAddressList) {
            if (StringUtils.equals("InternalIP", address.getType())) {
                internalIp = address.getAddress();
            }
        }
        return internalIp;
    }

    /**
     * 获取node hostname
     *
     * @param nodeAddressList
     * @return
     */
    public static String getNodeHostName(List<NodeAddress> nodeAddressList) {
        String hostname = null;
        for (NodeAddress address : nodeAddressList) {
            if (StringUtils.equals("Hostname", address.getType())) {
                hostname = address.getAddress();
            }
        }
        return hostname;
    }
}
