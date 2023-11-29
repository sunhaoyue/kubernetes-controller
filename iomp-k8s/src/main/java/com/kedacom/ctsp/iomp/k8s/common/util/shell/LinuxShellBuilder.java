package com.kedacom.ctsp.iomp.k8s.common.util.shell;


import com.kedacom.ctsp.iomp.k8s.common.util.AbstractShellBuilder;

public class LinuxShellBuilder extends AbstractShellBuilder {

    @Override
    public Shells buildTextByFile(String text) throws Exception {
        if (!text.startsWith("#!")) {
            text = "#!/usr/bin/env bash\n" + text;
        }
        String file = createFile(text);
        return Shells.build("bash", file);
    }

    @Override
    public Shells buildTextShell(String text) {
        return Shells.build(text);
    }


}
