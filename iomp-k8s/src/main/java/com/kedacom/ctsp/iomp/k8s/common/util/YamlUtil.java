package com.kedacom.ctsp.iomp.k8s.common.util;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
/**
 * yaml处理工具
 **/
public class YamlUtil {

    /**
     * yaml转为对象
     * <p>
     *
     * @param yamlStr 字符串
     * @return
     */
    public static <E> E convertYamlToObj(String yamlStr, Class<E> type) {
        Yaml yaml = new Yaml();
        E ret = yaml.loadAs(yamlStr, type);
        return ret;
    }

    /**
     * 对象转yaml字符串
     * <p>
     *
     * @param obj Object 要转换的对象
     * @return
     */
    public static String convertObjToYaml(Object obj) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); // 设置YAML输出格式

        Representer representer = new Representer();
        representer.addClassTag(obj.getClass(), Tag.MAP); // 设置类型标签

        Yaml yaml = new Yaml(representer, options);
        return yaml.dump(obj);
    }
    public static String convertObjToYaml1(Object obj) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); // 设置YAML输出格式
        Representer representer = new Representer();
        representer.addClassTag(obj.getClass(), Tag.MAP); // 设置类型标签
        Yaml yaml = new Yaml(representer, options);
        String yamlString = yaml.dump(obj);

        // Replace null values with empty strings
        yamlString = yamlString.replaceAll(": null", ": ''");

        // Apply proper indentation
        StringBuilder indentedYaml = new StringBuilder();
        String[] lines = yamlString.split(System.lineSeparator());
        int indentLevel = 0;
        for (String line : lines) {
            if (line.startsWith("- ")) {
                indentLevel--;
            }
            /*for (int i = 0; i < indentLevel; i++) {
                indentedYaml.append("  ");
            }*/
            indentedYaml.append(line).append(System.lineSeparator());
            if (line.endsWith(":")) {
                indentLevel++;
            }
        }

        return indentedYaml.toString();
    }

    /**
     * 串转为yaml文件
     *
     * @param context
     * @return
     * @throws IOException
     */
    public static File convert2File(Object context, String path) throws IOException {
        Yaml yaml = new Yaml();
        File file = new File(path);
        if (!file.exists()) {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
        }
        FileWriter fileWriter = new FileWriter(file);
        if (context instanceof String) {
            context = yaml.load((String) context);
        }
        yaml.dump(context, fileWriter);
        fileWriter.close();
        return file;
    }

    public static void main(String[] args) throws IOException {
        String s = "dolphinVersion: iomp-version-2.0.0\n" +
                "clientId: tomcat\n" +
                "replicas: '1'\n" +
                "namespace: abc\n" +
                "appVersion : 1.0.0\n" +
                "app: tomcat\n" +
                "annotations: null\n" +
                "containers:\n" +
                "  project:\n" +
                "    image: docker.kedacom.com:15000/dolphin/tomcat_oraclejdk:8.5.27\n" +
                "  base:\n" +
                "    image: docker.kedacom.com:15000/dolphin/tomcat_oraclejdk:8.5.27\n" +
                "    resources:\n" +
                "      requests:\n" +
                "        memory: 256Mi\n" +
                "        cpu: 100m\n" +
                "      limits:\n" +
                "        memory: 256Mi\n" +
                "        cpu: 1000m\n" +
                "    ports:\n" +
                "    - containerPort: '6379'\n" +
                "      name: svcpname0\n" +
                "      hostPort: '6379'\n" +
                "      protocol: TCP\n" +
                "    pullPolicy: IfNotPresent\n" +
                "    probe:\n" +
                "      livenessProbe:\n" +
                "        failureThreshold: 3\n" +
                "        periodSeconds: 10\n" +
                "        timeoutSeconds: 3\n" +
                "        tcpSocket:\n" +
                "          port: 6379\n" +
                "        successThreshold: 1\n" +
                "        initialDelaySeconds: 600\n" +
                "      readinessProbe:\n" +
                "        failureThreshold: 3\n" +
                "        periodSeconds: 10\n" +
                "        timeoutSeconds: 3\n" +
                "        tcpSocket:\n" +
                "          port: 6379\n" +
                "        successThreshold: 1\n" +
                "        initialDelaySeconds: 30\n" +
                "  clog:\n" +
                "    image: docker.kedacom.com:15000/fluent-bit:1.2.1\n" +
                "    pullPolicy: IfNotPresent\n" +
                "serviceAccount: admin\n" +
                "env:\n" +
                "- name: PROJECT_NAME\n" +
                "  value: tomcat\n" +
                "- name: CONFIG_NS\n" +
                "  value: abc\n" +
                "- name: DOLPHIN_TIMESTAMP\n" +
                "  value: '1593770609920'\n" +
                "- name: LOG_NODE_NAME\n" +
                "  valueFrom:\n" +
                "    fieldRef:\n" +
                "      apiVersion: v1\n" +
                "      fieldPath: spec.nodeName\n" +
                "- name: LOG_HOST_NAME\n" +
                "  valueFrom:\n" +
                "    fieldRef:\n" +
                "      apiVersion: v1\n" +
                "      fieldPath: metadata.name\n" +
                "- name: tomcat-name\n" +
                "  value: tangjixing-test\n" +
                "- name: vip\n" +
                "  value: ''\n" +
                "strategy: \n" +
                "deployName: tomcat\n" +
                "labels:\n" +
                "  dolphin/client_id: tomcat\n" +
                "  app.kubernetes.io/managed-by: iomp-helm\n" +
                "  app.kubernetes.io/name: tomcat\n" +
                "  dolphin/sign: tomcat\n" +
                "  app.kubernetes.io/instance: tomcat\n" +
                "  app.kubernetes.io/version: 8.5.27\n" +
                "  app.kubernetes.io/component: tomcat\n" +
                "\n";
//        String s1 = convertObjToYaml(s);
//        System.out.println(s1);
//        convertStr2File(s, "D:\\tet.yaml");

        Yaml yaml = new Yaml();
        Object load = yaml.load(s);
        System.out.println(load);
        convert2File(load, "D:\\tet.yaml");
    }
}
