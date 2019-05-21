#!/bin/sh

javac -d out/helloclient_build -cp artifacts/info.kgeorgiy.java.advanced.hello.jar java/ru.ifmo.rain.shelepov/hello/*.java
java -cp out/helloclient_build -p artifacts:lib -m info.kgeorgiy.java.advanced.hello client-i18n ru.ifmo.rain.shelepov.hello.HelloUDPClient 123
