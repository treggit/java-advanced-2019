OUT="out/implementor_build"
LIB="lib"
ARTIFACT="artifacts/info.kgeorgiy.java.advanced.implementor.jar"
TARGET="ru.ifmo.rain.shelepov/implementor/Implementor.java"
TARGET_CLASSPATH="ru/ifmo/rain/shelepov/implementor/Implementor.class"
PACKAGE="info/kgeorgiy/java/advanced/implementor"
MANIFEST="Manifest.txt"

mkdir $OUT
javac -d $OUT -cp $ARTIFACT java/$TARGET
jar xf $ARTIFACT $PACKAGE/ImplerException.class $PACKAGE/JarImpler.class $PACKAGE/Impler.class
cd $OUT
jar cfm Implementor.jar ../../$MANIFEST $TARGET_CLASSPATH ../../$PACKAGE/*.class
cd ../../