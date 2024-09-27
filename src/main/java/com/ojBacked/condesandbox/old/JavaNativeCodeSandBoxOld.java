package com.ojBacked.condesandbox.old;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import com.ojBacked.condesandbox.CodeSandBox;
import com.ojBacked.condesandbox.model.ExecuteCodeRequest;
import com.ojBacked.condesandbox.model.ExecuteCodeResponse;
import com.ojBacked.condesandbox.model.ExecuteMessage;
import com.ojBacked.condesandbox.model.JudgeInfo;
import com.ojBacked.condesandbox.utils.ProcessUtil;


import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class JavaNativeCodeSandBoxOld implements CodeSandBox {

    private static final String  GLOBAL_CODE_DIR_NAME= "tmpCode";
    private static final String  GLOBAL_JAVA_CLASS_NAME= "Main.java";

    //线程最大运行时间
    private static final long TIME_OUT =5000;

    private static final WordTree wordTree;
    private static final List<String> blackList = Arrays.asList("File","exec");

    static{
        wordTree = new WordTree();
        wordTree.addWords(blackList);
    }


    public static void main(String[] args) {
        JavaNativeCodeSandBoxOld javaNativeCodeSandBoxOld =new JavaNativeCodeSandBoxOld();
        ExecuteCodeRequest executeCodeRequest =new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2","3 3"));
        //读取文件中代码
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
//        String code = ResourceUtil.readStr("testCode/simpleComputeMemoryError/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandBoxOld.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();


        //todo : 校验代码
        //  校验代码中是否包含黑名单中的禁用词
        FoundWord foundWord = wordTree.matchWord(code);
        if (foundWord != null) {
            System.out.println("包含禁止词：" + foundWord.getFoundWord());
            return null;
        }


        //拿到当前工作目录
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        if (!FileUtil.exist(globalCodePathName)){
            FileUtil.mkdir(globalCodePathName);
        }

        //代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();

        //将代码写入文件
        String userCodePath =userCodeParentPath+ File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);


        //编译代码文件(.java)，得到class文件
        String compute = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compute);
            ExecuteMessage executeMessage = ProcessUtil.runProcessAndGetMessage(compileProcess, "编译");
            System.out.println(executeMessage);
            compileProcess.destroy();
            if(executeMessage.getExitValue()!=0){
                return getErrorResponse(executeMessage.getErrorMessage());
            }
        } catch (IOException e) {
            return getErrorResponse("编译错误");
        }

        //执行程序
        List<ExecuteMessage> executeMessages =new ArrayList<>();
        for (String inputArgs : inputList) {
            String runCmd = String.format("java -Dfile.encoding=UTF-8 -cp %s Main %s",userCodeParentPath,inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                //超时控制
                new Thread(()->{
                    try {
                        Thread.sleep(TIME_OUT);
                        System.out.println("超时了");
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtil.runProcessAndGetMessage(runProcess, "运行");
                System.out.println(executeMessage);
                executeMessages.add(executeMessage);
            } catch (IOException e) {
                return getErrorResponse("代码执行错误过程");
            }
        }

        //收集执行结果信息
        ExecuteCodeResponse response =new ExecuteCodeResponse();
        List<String> outputList =new ArrayList<>();
        response.setStatue(2);

        Long maxTime = 0l;
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
        }
        response.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);

        response.setJudgeInfo(judgeInfo);

        //文件清理
        if (userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
        }
        return response;
    }

    public ExecuteCodeResponse getErrorResponse (String message){
        ExecuteCodeResponse response = new ExecuteCodeResponse();
        response.setMessage(message);
        response.setOutputList(new ArrayList<>());
        response.setStatue(3);
        response.setJudgeInfo(new JudgeInfo());
        return response;
    }
}
