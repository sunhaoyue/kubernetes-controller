package com.kedacom.ctsp.iomp.k8s.common.util.shell;

/**
 * Created by zhouhao on 16-6-28.
 */
public interface ShellBuilder {
    Shells buildTextByFile(String text) throws Exception;

    Shells buildTextShell(String text);

}
