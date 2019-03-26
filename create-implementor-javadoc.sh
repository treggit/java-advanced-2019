OUT="out/implementor_build/javadoc"
ARTIFACT="artifacts/info.kgeorgiy.java.advanced.implementor.jar"
ORACLE="https://docs.oracle.com/javase/10/docs/api/"
PACKAGE="ru.ifmo.rain.shelepov.implementor"
KGEORGIY_PACKAGE="modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor"
LIB="lib/*"

javadoc -d $OUT -link $ORACLE -cp java/:$LIB:$ARTIFACT:modules -private $PACKAGE java/ru.ifmo.rain.shelepov/implementor/Implementor.java $KGEORGIY_PACKAGE/Impler.java $KGEORGIY_PACKAGE/JarImpler.java $KGEORGIY_PACKAGE/ImplerException.java
