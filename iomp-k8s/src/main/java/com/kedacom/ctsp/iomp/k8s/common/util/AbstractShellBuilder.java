package com.kedacom.ctsp.iomp.k8s.common.util;

import com.kedacom.ctsp.iomp.k8s.common.util.shell.ShellBuilder;
import com.kedacom.ctsp.lang.StringUtil;
import com.kedacom.ctsp.lang.file.FileUtils;

import java.io.File;

public abstract class AbstractShellBuilder implements ShellBuilder {
    protected String createFile(String text) throws Exception {
        String tmp = System.getProperty("java.io.tmpdir").concat("/org/kedacom/shell/");
        File file = new File(tmp);
        file.mkdirs();
        String timeStr = StringUtil.concat("shell_", Math.abs(text.hashCode()));
        String shellFileName = timeStr + OsType.os.extension;
        String encode = OsType.os.encode;
        FileUtils.writeString2File(text, shellFileName = new File(file, shellFileName).getAbsolutePath(), encode);
        return shellFileName;
    }

    enum OsType {
        WINDOWS("gbk", ".bat"), LINUX("utf-8", ".sh"), MACOS("utf-8", ".sh");

        public final String encode;
        public final String extension;
        public static OsType os;

        static {
            String osName = System.getProperty("os.name").toLowerCase();
            OsType now = null;
            if (osName.contains("win")) {
                now = OsType.WINDOWS;
            } else if (osName.contains("linux")) {
                now = OsType.LINUX;
            } else if (osName.contains("mac")) {
                now = OsType.MACOS;
            }
            OsType.os = now;
        }

        OsType(String encode, String extension) {
            this.encode = encode;
            this.extension = extension;
        }
    }
}
