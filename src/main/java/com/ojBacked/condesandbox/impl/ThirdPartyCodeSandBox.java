package com.ojBacked.condesandbox.impl;


import com.ojBacked.condesandbox.CodeSandBox;
import com.ojBacked.condesandbox.model.ExecuteCodeRequest;
import com.ojBacked.condesandbox.model.ExecuteCodeResponse;

/**
 * 调用第三方代码沙箱。
 */
public class ThirdPartyCodeSandBox implements CodeSandBox {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.out.println("第三方代码沙箱测试");
        return null;
    }
}
