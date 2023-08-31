package com.atguigu.gmall.common.cache;

import java.lang.annotation.*;


/**
 * 元注解：修饰注解的注解
 *@Target: 目标位置
 *    TYPE:类
 *    METHOD:方法
 *    可以参考：ElementType
 *@Retention：生命周期
 *
 *    SOURCE：源码
 *    CLASS ：字节码
 *   RUNTIME：运行时期
 * @Inherited
 *    父子类对注解继承关系
 *@Documented
 *  javadoc 生成api文档
 *
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface GmallCache {


    String prefix() default "cache:";
    String suffix() default ":info";
}
