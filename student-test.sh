javac -d out/student_build -cp "artifacts/info.kgeorgiy.java.advanced.student.jar" java/ru.ifmo.rain.shelepov/student/StudentDB.java
java -cp out/student_build/ -p artifacts:lib -m info.kgeorgiy.java.advanced.student StudentGroupQuery ru.ifmo.rain.shelepov.student.StudentDB
