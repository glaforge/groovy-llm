package com.google.cloud.devrel.docchat;

import io.micronaut.runtime.Micronaut;

public class Application {

    public static void main(String[] args) {
//        Micronaut.run(Application.class, args);
        Micronaut.build(args)
            .eagerInitSingletons(true) // eagerly initialize the LLMQueryService singleton to load the vector DB
            .mainClass(Application.class)
            .start();
    }
}