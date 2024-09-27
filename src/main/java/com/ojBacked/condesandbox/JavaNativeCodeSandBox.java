package com.ojBacked.condesandbox;

import com.ojBacked.condesandbox.model.ExecuteCodeRequest;
import com.ojBacked.condesandbox.model.ExecuteCodeResponse;
import com.ojBacked.condesandbox.model.ExecuteMessage;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
public class JavaNativeCodeSandBox extends JavaCodeSandBoxTemplate{
    @Override
    public File writeCodeToFile(String code) {
        return super.writeCodeToFile(code);
    }

    @Override
    public ExecuteMessage codeCompile(File userCodeFile) {
        return super.codeCompile(userCodeFile);
    }

    @Override
    public List<ExecuteMessage> codeExec(List<String> inputList, File userCodeFile) {
        return super.codeExec(inputList, userCodeFile);
    }

    @Override
    public ExecuteCodeResponse getExecuteCodeResponse(List<ExecuteMessage> executeMessages) {
        return super.getExecuteCodeResponse(executeMessages);
    }

    @Override
    public boolean fileClear(File userCodeFile) {
        return super.fileClear(userCodeFile);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }

    @Override
    public ExecuteCodeResponse getErrorResponse(String message) {
        return super.getErrorResponse(message);
    }
}
