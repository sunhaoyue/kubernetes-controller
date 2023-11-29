package com.kedacom.ctsp.iomp.k8s.common.util.shell;

import lombok.extern.slf4j.Slf4j;

/**
 * @author tangjixing
 * @date 2019/3/7
 */
@Slf4j
public class CmdExecUtil {

    public static String INFO = "info";
    public static String ERROR = "error";

    public static String execGetInfo(String command) {
        System.out.println("exec command:" + command);
        StringBuilder sb = new StringBuilder();
        try {
            Shells.buildText(command)
                    .onProcess((line, helper) -> {
                        System.out.println("CmdExecUtil.line:" + line);
                        sb.append(line);
                        sb.append("\n");
                    }).onError((line, helper) -> {
                        System.out.println("exec error:" + line);
                    }).exec();
            return sb.toString();
        } catch (Exception e) {
            log.error("exec exception", e);
            e.printStackTrace();
        }
        return null;
    }

}
