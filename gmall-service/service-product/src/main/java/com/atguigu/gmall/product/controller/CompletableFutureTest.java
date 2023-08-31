package com.atguigu.gmall.product.controller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class CompletableFutureTest {


    public static void main(String[] args) throws ExecutionException, InterruptedException {
//
//        //创建异步对象
//        CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
//
//            System.out.println("我是没有返回值的异步对象");
//
//        });
//        System.out.println(completableFuture.get());



        //创建异步对象
//        CompletableFuture<Integer> completableFuture = CompletableFuture.supplyAsync(() -> {
//
//            System.out.println("我是有返回值的异步对象");
//
//            int i=1/0;
//            return 404;
//        }).whenCompleteAsync(new BiConsumer<Integer, Throwable>() {
//            @Override
//            public void accept(Integer integer, Throwable throwable) {
//
//
//                System.out.println("integer = " + integer + ", throwable = " + throwable);
//
//
//            }
//        }).exceptionally(new Function<Throwable, Integer>() {
//            @Override
//            public Integer apply(Throwable throwable) {
//
//                System.out.println(throwable.getMessage());
//
//                return null;
//            }
//        });


//        System.out.println(completableFuture.get());



        //创建线程A
        CompletableFuture<Integer> futureA = CompletableFuture.supplyAsync(() -> {

            System.out.println("我是A.....");

            return 1024;
        });

        //创建线程对象B
        CompletableFuture<Void> futureB = futureA.thenAcceptAsync(num -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            System.out.println("我是B。。。。。"+num);

        });
        //创建线程对象C
        CompletableFuture<Void> futureC = futureA.thenAcceptAsync(num -> {

            System.out.println("我是C。。。。。"+num);

        });

        futureA.join();
        futureB.join();
        futureC.join();

    }
}
