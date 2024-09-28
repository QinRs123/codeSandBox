package com.ojBacked.condesandbox;

import cn.hutool.core.io.resource.ResourceUtil;
import com.ojBacked.condesandbox.model.ExecuteCodeRequest;
import com.ojBacked.condesandbox.model.ExecuteCodeResponse;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class TestTemplate {
    public static void main(String[] args) {
        JavaDockerCodeSandBox javaDockerCodeSandBox = new JavaDockerCodeSandBox();
        ExecuteCodeRequest executeCodeRequest =new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2","3 3"));
        //读取文件中代码
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
//        String code = ResourceUtil.readStr("testCode/simpleComputeTimeOut/Main.java", StandardCharsets.UTF_8);
        //String code = ResourceUtil.readStr("testCode/simpleComputeMemoryError/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaDockerCodeSandBox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }
}
