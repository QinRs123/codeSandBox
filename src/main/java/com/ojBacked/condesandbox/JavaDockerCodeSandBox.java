package com.ojBacked.condesandbox;

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
import com.ojBacked.condesandbox.model.JudgeInfo;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class JavaDockerCodeSandBox extends JavaCodeSandBoxTemplate{

    private static final boolean NEED_PULL_IMAGE = false;
    private static final boolean NEED_CREATE_CONTAINER = false;
    //线程最大运行时间
    private static final long TIME_OUT =5000;

    private static final String CONTAINER_ID = "3a71640f42f5";
    @Override
    public List<ExecuteMessage> codeExec(List<String> inputList, File userCodeFile) {
        String image = "openjdk:8-alpine";
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        if(NEED_PULL_IMAGE){
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            System.out.println(pullImageCmd);
        }

        String containerId = CONTAINER_ID;
        //创建容器
        if(NEED_CREATE_CONTAINER){
            HostConfig hostConfig = new HostConfig();

            //globalCodePathName
            String absolutePath = userCodeFile.getParentFile().getParentFile().getAbsolutePath();
            hostConfig.setBinds(new Bind(absolutePath, new Volume("/app")));
            hostConfig.withMemory(100*1024*1000L);//100m
            hostConfig.withCpuCount(1L);
            CreateContainerResponse createContainerResponse = dockerClient.createContainerCmd(image)
                    .withHostConfig(hostConfig)
                    .withNetworkDisabled(true)
                    .withAttachStdin(true)
                    .withAttachStderr(true)
                    .withAttachStdout(true)
                    .withName("jdk8")
                    .withTty(true)
                    .exec();

            System.out.println(createContainerResponse);
            containerId = createContainerResponse.getId();
            System.out.println("容器创建完成，容器id为："+containerId);
            //启动容器
            dockerClient.startContainerCmd(containerId)
                    .exec();
            System.out.println("启动容器");
        }


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
//                    没超时
                    timout[0] = false;
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
            } catch (InterruptedException e) {
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
    public ExecuteCodeResponse getExecuteCodeResponse(List<ExecuteMessage> executeMessages) {
        ExecuteCodeResponse response =new ExecuteCodeResponse();
        List<String> outputList =new ArrayList<>();
        response.setStatue(2);

        Long maxTime = 0l;
        Long maxMemory = 0L;
        for (ExecuteMessage executeMessage : executeMessages) {
            if(executeMessage.getExitValue()!=0){
                response.setStatue(3);
                outputList.add(executeMessage.getErrorMessage());
            }else{
                outputList.add(executeMessage.getMessage());
            }
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
            Long memory = executeMessage.getMemory();
            if(memory!=null){
                maxMemory = Math.max(maxMemory,memory);
            }
        }
        response.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        judgeInfo.setMemory(maxMemory);
        response.setJudgeInfo(judgeInfo);
        return response;
    }

}
