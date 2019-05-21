#!/bin/sh

javac -d out/crawler_build -cp artifacts/info.kgeorgiy.java.advanced.crawler.jar java/ru.ifmo.rain.shelepov/crawler/*.java
java -cp out/crawler_build -p artifacts:lib -m info.kgeorgiy.java.advanced.crawler hard ru.ifmo.rain.shelepov.crawler.WebCrawler
