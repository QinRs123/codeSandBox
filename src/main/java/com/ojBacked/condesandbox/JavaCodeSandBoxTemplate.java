package com.ojBacked.condesandbox;

import cn.hutool.core.io.FileUtil;
import com.ojBacked.condesandbox.model.ExecuteCodeRequest;
import com.ojBacked.condesandbox.model.ExecuteCodeResponse;
import com.ojBacked.condesandbox.model.ExecuteMessage;
import com.ojBacked.condesandbox.model.JudgeInfo;
import com.ojBacked.condesandbox.utils.ProcessUtil;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public abstract class JavaCodeSandBoxTemplate implements CodeSandBox{



    private static final String  GLOBAL_CODE_DIR_NAME= "tmpCode";
    private static final String  GLOBAL_JAVA_CLASS_NAME= "Main.java";

    //线程最大运行时间
    private static final long TIME_OUT =5000;

    /**
     * 将代码写入指定文件
     * @param code
     * @return
     */
    public File writeCodeToFile(String code){
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
        return userCodeFile;
    }

    /**
     * 编译代码文件(.java)，得到class文件
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage codeCompile(File userCodeFile){
        String compute = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compute);
            ExecuteMessage executeMessage = ProcessUtil.runProcessAndGetMessage(compileProcess, "编译");
            compileProcess.destroy();
            return executeMessage;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行代码
     * @param inputList
     * @param userCodeFile
     * @return
     */
    public List<ExecuteMessage> codeExec (List<String> inputList, File userCodeFile){
        List<ExecuteMessage> executeMessages =new ArrayList<>();
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
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
                throw new RuntimeException(e);
            }
        }
        return executeMessages;
    }

    /**
     * 收集，整理返回信息
     * @param executeMessages
     * @return
     */
    public ExecuteCodeResponse getExecuteCodeResponse(List<ExecuteMessage> executeMessages) {
        ExecuteCodeResponse response =new ExecuteCodeResponse();
        List<String> outputList =new ArrayList<>();
        response.setStatue(2);

        Long maxTime = 0l;
        Long maxMemory = 0L;
        for (ExecuteMessage executeMessage : executeMessages) {
            if(executeMessage.getExitValue()==null || executeMessage.getExitValue()!=0 ){
                response.setStatue(4);
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

    public boolean fileClear(File userCodeFile){
        boolean del = true;
        if (userCodeFile.getParentFile() != null) {
            System.out.println("删除" + (del ? "成功" : "失败"));
            return del = FileUtil.del(userCodeFile.getParentFile().getAbsoluteFile());

        }
        return del;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        //将代码写入指定文件
        File userCodeFile = writeCodeToFile(code);

        //编译代码文件(.java)，得到class文件
        ExecuteMessage complieMessage = null;
        try {
            complieMessage = codeCompile(userCodeFile);
            if(complieMessage.getExitValue()!=0){
                return getErrorResponse(complieMessage.getErrorMessage());
            }
        } catch (Exception e) {
            return getErrorResponse(complieMessage.getErrorMessage());
        }
        //执行程序
        List<ExecuteMessage> executeMessages = null;
        try {
            executeMessages = codeExec(inputList, userCodeFile);
        } catch (Exception e) {
            return getErrorResponse("代码运行异常");
        }
        //收集执行结果信息
        ExecuteCodeResponse executeCodeResponse = getExecuteCodeResponse(executeMessages);

        //文件清理
        boolean b = fileClear(userCodeFile);
        return executeCodeResponse;
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
