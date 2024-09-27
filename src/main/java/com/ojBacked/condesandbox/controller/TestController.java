package com.ojBacked.condesandbox.controller;

import com.ojBacked.condesandbox.JavaNativeCodeSandBox;
import com.ojBacked.condesandbox.model.ExecuteCodeRequest;
import com.ojBacked.condesandbox.model.ExecuteCodeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/")
    public String test(){
        return "success....";
    }

    @Autowired
    private JavaNativeCodeSandBox javaNativeCodeSandBox;

//    ExecuteCodeResponse executeCodeResponse = codeSendBoxProxy.executeCode(executeCodeRequest);
    @PostMapping("/executeCode")
    public ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest){
        if (executeCodeRequest == null) {
            throw new RuntimeException("请求参数为空");
        }
//        JavaNativeCodeSandBox javaNativeCodeSandBox = new JavaNativeCodeSandBox();
        System.out.println(executeCodeRequest);
        return javaNativeCodeSandBox.executeCode(executeCodeRequest);
    }

}
