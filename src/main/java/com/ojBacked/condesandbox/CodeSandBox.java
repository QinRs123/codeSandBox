package com.ojBacked.condesandbox;


import com.ojBacked.condesandbox.model.ExecuteCodeRequest;
import com.ojBacked.condesandbox.model.ExecuteCodeResponse;

/**
 * 定义代码沙箱接口，提高通用性
 */
public interface CodeSandBox {
    /**
     * 执行代码
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
