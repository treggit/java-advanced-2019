OUT="out/implementor_build/javadoc"
ARTIFACT="artifacts/info.kgeorgiy.java.advanced.implementor.jar"
ORACLE="https://docs.oracle.com/en/java/javase/11/docs/api/"
PACKAGE="ru.ifmo.rain.shelepov.implementor"
KGEORGIY_PACKAGE="modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor"
LIB="lib/*"

javadoc -d $OUT -link $ORACLE --module-source-path modules -p modules/info.kgeorgiy.java.advanced.base.jar:modules/info.kgeorgiy.java.advanced.implementor.jar:\
lib/hamcrest-core-1.3.jar:lib/jsoup-1.8.1.jar:lib/junit-4.11.jar:lib/quickcheck-0.6.jar -private --module ${PACKAGE} $KGEORGIY_PACKAGE/Impler.java $KGEORGIY_PACKAGE/JarImpler.java $KGEORGIY_PACKAGE/ImplerException.java
