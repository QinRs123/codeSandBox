package com.ojBacked.condesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 题目判断信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JudgeInfo {

    /**
     * 执行信息
     */
    private String message;


    /**
     * 内存消耗
     */
    private Long memory;

    /**
     * 消耗时间
     */
    private Long time;

    /**
     * 详细信息
     */
    private List<String> details;
}
