package com.ojBacked.condesandbox.impl;


import com.ojBacked.condesandbox.CodeSandBox;
import com.ojBacked.condesandbox.model.ExecuteCodeRequest;
import com.ojBacked.condesandbox.model.ExecuteCodeResponse;

/**
 * 远程调用代码沙箱，实际调用接口的
 */
public class RemoteCodeSandBox implements CodeSandBox {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.out.println("远程代码沙箱测试");
        return null;
    }
}
