package com.xxxx.seckill;




import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WhateverTest {

    public void testAdd() {
        // https://tobebetterjavaer.com/jvm/how-jvm-run-zijiema-zhiling.html
        add(1,2);
    }

    private int add(int a, int b) {
        return a + b;
    }

    public static void main(String args[]){
        String s1 = "Runoob";              // String 直接创建
        String s2 = "Runoob";              // String 直接创建
        String s3 = s1;                    // 相同引用
        String s4 = new String("Runoob");   // String 对象创建
        String s5 = new String("Runoob");   // String 对象创建

        Integer i1 = 1;
        // 看不出来是堆还是什么公共池的

        ArrayList arrayList = new ArrayList();
        System.out.println("");
    }
}
