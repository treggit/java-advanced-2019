#!/bin/sh

javac -d out/mapper_build -cp artifacts/info.kgeorgiy.java.advanced.mapper.jar:artifacts/info.kgeorgiy.java.advanced.concurrent.jar java/ru.ifmo.rain.shelepov/concurrent/ParallelMapperImpl.java java/ru.ifmo.rain.shelepov/concurrent/IterativeParallelism.java
java -cp out/mapper_build -p artifacts:lib -m info.kgeorgiy.java.advanced.mapper list ru.ifmo.rain.shelepov.concurrent.ParallelMapperImpl,ru.ifmo.rain.shelepov.concurrent.IterativeParallelism 123
