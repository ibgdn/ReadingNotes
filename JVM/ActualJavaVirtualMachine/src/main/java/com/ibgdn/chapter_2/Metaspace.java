package com.ibgdn.chapter_2;

import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Dispatcher;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.lang.reflect.Method;
import java.security.BasicPermission;

/**
 * 元数据区溢出
 * <p>
 * VM options：
 * -XX:+PrintGCDetails -XX:MetaspaceSize=5m -XX:MaxMetaspaceSize=10m
 */
public class Metaspace {
    public static void main(String[] args) {
        for (int i = 0; i < 100000; i++) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(BasicPermission.class);
            enhancer.setCallbackTypes(new Class[]{Dispatcher.class, MethodInterceptor.class});
            enhancer.setCallbackFilter(new CallbackFilter() {
                public int accept(Method method) {
                    return 0;
                }
            });

            Class aClass = enhancer.createClass();
        }
    }
}
