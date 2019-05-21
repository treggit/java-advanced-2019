#!/bin/sh

javac -d out/helloserver_build -cp artifacts/info.kgeorgiy.java.advanced.hello.jar java/ru.ifmo.rain.shelepov/hello/*.java
java -cp out/helloserver_build -p artifacts:lib -m info.kgeorgiy.java.advanced.hello server-i18n ru.ifmo.rain.shelepov.hello.HelloUDPServer 123
