package com.ojBacked.condesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.StatsCmd;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.ojBacked.condesandbox.model.ExecuteCodeResponse;
import com.ojBacked.condesandbox.model.ExecuteMessage;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class JavaDockerCodeSandBox extends JavaCodeSandBoxTemplate implements DisposableBean {

    private static final boolean NEED_PULL_IMAGE = false;
    private static final boolean NEED_CREATE_CONTAINER = true;
    //线程最大运行时间
    private static final long TIME_OUT =5000;

    private static  String CONTAINER_ID = "";

    private static String CONTAINER_NAME = "jdk8-test";

    private static DockerClient dockerClient = null;
    private static final String  GLOBAL_CODE_DIR_NAME= "tmpCode";
    private static final String  GLOBAL_JAVA_CLASS_NAME= "Main.java";

    private static String globalCodePathName = null;

    static {
        dockerClient = DockerClientBuilder.getInstance().build();
        String userDir = System.getProperty("user.dir");
        globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        if (!FileUtil.exist(globalCodePathName)){
            FileUtil.mkdir(globalCodePathName);
        }

        String image = "openjdk:8-alpine";
        if(NEED_PULL_IMAGE){
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            System.out.println(pullImageCmd);
        }
        if(NEED_CREATE_CONTAINER){
            HostConfig hostConfig = new HostConfig();

            hostConfig.setBinds(new Bind(globalCodePathName, new Volume("/app")));
            hostConfig.withMemory(100*1024*1000L);//100m
            hostConfig.withCpuCount(1L);
            CreateContainerResponse createContainerResponse = dockerClient.createContainerCmd(image)
                    .withHostConfig(hostConfig)
                    .withNetworkDisabled(true)
                    .withAttachStdin(true)
                    .withAttachStderr(true)
                    .withAttachStdout(true)
                    .withName("jdk8-test")
                    .withTty(true)
                    .exec();

            System.out.println(createContainerResponse);
            CONTAINER_ID = createContainerResponse.getId();
            System.out.println("容器创建完成，容器id为："+CONTAINER_ID);
        }

    }


    /**
     * 将代码写入指定文件
     * @param code
     * @return
     */
    @Override
    public File writeCodeToFile(String code){
        //代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        //将代码写入文件
        String userCodePath =userCodeParentPath+ File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }
    @Override
    public ExecuteMessage codeCompile(File userCodeFile) {
//        String image = "openjdk:8-alpine";

        String containerId = CONTAINER_ID;
        //创建容器

        System.out.println("启动容器");
        dockerClient.startContainerCmd(containerId)
                .exec();

        ExecuteMessage executeMessage = new ExecuteMessage();
        //编译代码
        String javaCode = userCodeFile.getParentFile().getAbsolutePath();
        String[] javaSplit = javaCode.split(File.separator);
        String java = javaSplit[javaSplit.length - 1];
        String[] compile = ArrayUtil.append(new String[]{"javac","/app/"+java+"/Main.java"});
        ExecCreateCmdResponse compileCmd = dockerClient.execCreateCmd(containerId)
                .withCmd(compile)
                .withAttachStderr(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withTty(true)
                .exec();

        String compileCmdId = compileCmd.getId();

        executeMessage.setExitValue(0);
        ExecStartResultCallback execStartCompileResponse = new ExecStartResultCallback() {
            @Override
            public void onNext(Frame frame) {
                StreamType streamType = frame.getStreamType();
                if (StreamType.STDERR.equals(streamType)) {
                    System.out.println("输出错误结果：" + new String(frame.getPayload()));
                    executeMessage.setErrorMessage(new String(frame.getPayload()));
                    executeMessage.setExitValue(1);
                } else {
                    System.out.println("输出结果：" + new String(frame.getPayload()));
                    executeMessage.setExitValue(1);
                    executeMessage.setMessage(new String(frame.getPayload()));
                }
                super.onNext(frame);
            }
        };
        try {
            dockerClient.execStartCmd(compileCmdId)
                    .exec(execStartCompileResponse)
                    .awaitCompletion();
            execStartCompileResponse.close();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
        return executeMessage;
    }



    @Override
    public List<ExecuteMessage> codeExec(List<String> inputList, File userCodeFile) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        String containerId = CONTAINER_ID;
        List<ExecuteMessage> executeMessages =new ArrayList<>();
        for (String inputArgs : inputList) {
            //执行代码 docker exec jdk8 java -cp /app Main 5 8
            //创建命令
            String absolutePath = userCodeFile.getParentFile().getAbsolutePath();
            String[] split = absolutePath.split(File.separator);
            String s = split[split.length - 1];
            String[] input = inputArgs.split(" ");
            String[] cmd = ArrayUtil.append(new String[]{"java","-cp","/app/"+s,"Main"},input);
            ExecCreateCmdResponse createCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmd)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .withTty(true)
                    .exec();

            Long[] maxMemory = {0L};
            //获取占用的内存
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            ResultCallback<Statistics> exec = statsCmd.exec(new ResultCallback<Statistics>() {
                @Override
                public void onNext(Statistics statistics) {
                    Long memory = statistics.getMemoryStats().getUsage();
                    System.out.println("使用内存："+memory);
                    maxMemory[0] = Math.max(memory,maxMemory[0]);
                }
                @Override
                public void onStart(Closeable closeable) {}
                @Override
                public void onError(Throwable throwable) {}
                @Override
                public void onComplete() {}
                @Override
                public void close() throws IOException {}
            });
            statsCmd.exec(exec);

            ExecuteMessage executeMessage = new ExecuteMessage();
            StopWatch stopWatch = new StopWatch();
            final String[] message = {null};
            final String[] errorMessage = {null};
            final Integer[] exitValue = {null};
            final Boolean[] timout = {true};
            // 执行 命令
            String execId = createCmdResponse.getId();
            //创建回调函数
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onComplete() {
                    super.onComplete();
                    timout[0] = false;//                    没超时
                }
                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        message[0] = new String(frame.getPayload());
                        exitValue[0] = 1;
                        System.out.println("输出错误结果：" + new String(frame.getPayload()));
                    } else {
                        exitValue[0] = 0;
                        errorMessage[0] = new String(frame.getPayload());
                        System.out.println("输出结果：" + new String(frame.getPayload()));
                    }
                    super.onNext(frame);
                }
            };
            try {
                //记录时间
                stopWatch.start();
                dockerClient.execStartCmd(execId)
                        .exec(execStartResultCallback)
                        .awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS); //
                stopWatch.stop();
                statsCmd.close();
                exec.close();
                execStartResultCallback.close();
            } catch (InterruptedException | IOException e) {
                System.out.println("执行代码异常。。。。");
                throw new RuntimeException(e);
            }
            //记录时间
            executeMessage.setTime(stopWatch.getTotalTimeMillis());
            executeMessage.setErrorMessage(message[0]);
            executeMessage.setMessage(errorMessage[0]);
            executeMessage.setExitValue(exitValue[0]);
            //记录内存
            executeMessage.setMemory(maxMemory[0]);
            //获取结果
            executeMessages.add(executeMessage);
        }
        return executeMessages;
    }


    @Override
    public boolean fileClear(File userCodeFile) {
        dockerClient.stopContainerCmd(CONTAINER_ID).exec();
        return super.fileClear(userCodeFile);
    }

    @Override
    public ExecuteCodeResponse getErrorResponse(String message) {
        dockerClient.stopContainerCmd(CONTAINER_ID)
                .exec();
        System.out.println("编译失败，停止容器");
        return super.getErrorResponse(message);
    }

    @Override
    public void destroy() throws Exception {
        dockerClient.removeContainerCmd(CONTAINER_ID).withForce(true).exec();
    }
}
