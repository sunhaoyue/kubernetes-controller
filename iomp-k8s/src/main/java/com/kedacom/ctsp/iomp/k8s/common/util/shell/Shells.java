package com.kedacom.ctsp.iomp.k8s.common.util.shell;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.kedacom.ctsp.shell.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * 脚本执行器
 * Created by zhouhao on 16-6-28.
 */
@Slf4j
public class Shells {
    //默认字符集
    private static final String DEFAULT_ENCODE;

    private static ExecutorService executorService =
            new ThreadPoolExecutor(3, 128, 60L, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), new ThreadFactoryBuilder().setNameFormat("Shell-pool-%d").build());

    private String encode;

    private List<String> env;

    private List<String> commands;

    private List<Callback> processCallback = new LinkedList<>();

    private List<Callback> errorCallback = new LinkedList<>();

    private List<Consumer<ProcessHelper>> execBeforeCallback = new LinkedList<>();

    private static ShellBuilder shellBuilder;

    private boolean shutdown = false;

    private File dir;

    private ProcessHelper helper;

    private OutputStream stdin;

    static {
        String os = System.getProperty("os.name");
        if (StringUtils.startsWith(StringUtils.lowerCase(os), "win")) {
            DEFAULT_ENCODE = "gbk";
            //shellBuilder = new WindowsShellBuilder();
        } else {
            DEFAULT_ENCODE = "utf-8";
            shellBuilder = new LinuxShellBuilder();
        }
    }

    public Shells(String command, String... more) {
        encode = DEFAULT_ENCODE;
        commands = new ArrayList<>(Arrays.asList(command));
        commands.addAll(Arrays.asList(more));
        dir = new File("./");
        helper = new ProcessHelper() {

            @Override
            public void kill() {
                shutdown = true;
            }

            @Override
            public void sendMessage(byte[] msg) throws IOException {
                if (null != stdin) {
                    stdin.write(msg);
                    stdin.flush();
                }
            }
        };
    }

    public static Shells build(String command, String... more) {
        return new Shells(command, more);
    }

    /**
     * 执行命令
     *   执行方式: 将text 生成文件后执行
     *      windows下生成bat
     *      linux下生成sh
     *
     * @param text
     * @return
     * @throws Exception
     */
    public static Shells buildTextByFile(String text) throws Exception {
        return shellBuilder.buildTextByFile(text);
    }

    /**
     * 执行命令
     *
     *
     * @param text
     * @return
     */
    public static Shells buildText(String text) {
        return shellBuilder.buildTextShell(text);
    }

    public Shells dir(File file) {
        dir = file;
        return this;
    }

    public Shells dir(String dir) {
        return dir(new File(dir));
    }

    public Shells encode(String encode) {
        this.encode = encode;
        return this;
    }

    public Shells env(String... env) {
        if (this.env == null) {
            this.env = Arrays.asList(env);
        } else {
            this.env.addAll(Arrays.asList(env));
        }
        return this;
    }

    public Shells onProcess(Callback callback) {
        processCallback.add(callback);
        return this;
    }

    public Shells onError(Callback callback) {
        errorCallback.add(callback);
        return this;
    }

    public Shells before(Consumer<ProcessHelper> consumer) {
        execBeforeCallback.add(consumer);
        return this;
    }

    public Result exec() throws IOException {
        if (this.commands.size() > 1) {
            String[] envp;
            if (this.env == null || this.env.isEmpty()) {
                envp = new String[0];
            } else {
                envp = this.env.toArray(new String[this.env.size()]);
            }

            Process process = Runtime.getRuntime()
                    .exec(this.commands.toArray(new String[this.commands.size()])
                            , envp, dir);
            return process(process, encode);
        } else {
            log.info("shell 执行命令:{}", this.commands.get(0));
            Process process = Runtime.getRuntime()
                    .exec(this.commands.get(0));
            return process(process, encode);
        }
    }

    public Future<Result> execAsyn() {
        return executorService.submit(this::exec);
    }

    public void execAsyn(Consumer<Result> consumer) {
        executorService.execute(() -> {
            try {
                consumer.accept(exec());
            } catch (IOException e) {
                consumer.accept(new Result(-1, e, e.getMessage()));
            }
        });
    }

    private Result process(final Process process, String encode) {
        try (InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream(), encode);
             BufferedReader reader = new BufferedReader(inputStreamReader)) {
            log.info("Acquire inputStream:{}", this.commands.get(0));
            stdin = process.getOutputStream();
            new Thread(() -> {
                try (InputStreamReader errorReader = new InputStreamReader(process.getErrorStream(), encode);
                     BufferedReader errorStream = new BufferedReader(errorReader)) {
                    String line;
                    while ((line = errorStream.readLine()) != null) {
                        if (shutdown) {
                            process.destroyForcibly();
                        }
                        String tmp = line;
                        errorCallback.forEach(consumer -> consumer.accept(tmp, helper));
                    }
                } catch (final Exception e) {
                    try {
                        String errorMsg = "Failure to obtain error log, cmd=" + this.commands.get(0);
                        log.error(errorMsg, e);
                        errorCallback.forEach(consumer -> consumer.accept(errorMsg, helper));
                    } catch (Throwable t){}

                }
            }).start();
            log.info("before callback:{}", this.commands.get(0));
            execBeforeCallback.forEach(consumer -> consumer.accept(helper));
            log.info("readLine:{}", this.commands.get(0));
            String line;
            while ((line = reader.readLine()) != null) {
                if (shutdown) {
                    process.destroyForcibly();
                }
                String tmp = line;
                processCallback.forEach(consumer -> consumer.accept(tmp, helper));
            }
            return new Result(process.waitFor(), null, null);
        } catch (Exception e) {
            try {
                String errorMsg = "An exception occurred during the command execution., cmd=" + this.commands.get(0);
                log.error(errorMsg, e);
                errorCallback.forEach(consumer -> consumer.accept(errorMsg, helper));
            } catch (Throwable t){}
            return new Result(-1, e, e.getMessage());
        } finally {
            stdin = null;
        }
    }

}
