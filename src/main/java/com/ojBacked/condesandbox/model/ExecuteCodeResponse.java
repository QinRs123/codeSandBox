package com.ojBacked.condesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeResponse {

    /**
     * 接口信息
     */
    private String message;

    /**
     * 判题状态
     */
    private Integer statue;

    private List<String> outputList;

    /**
     * 判题信息
     */
    private JudgeInfo judgeInfo;
}
