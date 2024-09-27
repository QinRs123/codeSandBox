package com.ojBacked.condesandbox.utils;


import com.ojBacked.condesandbox.model.ExecuteMessage;
import org.springframework.util.StopWatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProcessUtil {

    public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String proName){

        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            StopWatch stopWatch =new StopWatch();
            stopWatch.start();
            int exitValue = runProcess.waitFor();
            StringBuilder compileOutputStringBuilder = new StringBuilder();
            String compileOutputLine;
            if(exitValue==0){
                System.out.println(proName+"成功");
                //读取信息
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                while((compileOutputLine= bufferedReader.readLine())!=null){
                    compileOutputStringBuilder.append(compileOutputLine);
                }
                executeMessage.setMessage(compileOutputStringBuilder.toString());
                bufferedReader.close();
            }else {
                System.out.println(proName+"失败，错误码："+exitValue);
                //要获取错误信息
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                while((compileOutputLine= bufferedReader.readLine())!=null){
                    compileOutputStringBuilder.append(compileOutputLine);
                }
                String errorMessage = compileOutputStringBuilder.toString();
//                String response;
//                if (errorMessage.contains("at")){
//                    String[] split = errorMessage.split("at ");
//                    response= split[0]+", "+split[split.length-1];
//                }else{
//                    response=errorMessage;
//                }
                executeMessage.setErrorMessage(errorMessage);
                bufferedReader.close();
            }
            stopWatch.stop();
            long totalTimeMillis = stopWatch.getTotalTimeMillis();
            executeMessage.setTime(totalTimeMillis);

            executeMessage.setExitValue(exitValue);
            System.out.println(compileOutputStringBuilder);
            return executeMessage;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
