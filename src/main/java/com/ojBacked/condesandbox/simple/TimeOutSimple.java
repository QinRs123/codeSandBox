package com.ojBacked.condesandbox.simple;

public class TimeOutSimple {

    public static void main(String[] args) {
        try {
            Thread.sleep(1000*60*70);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("end");
    }
}
