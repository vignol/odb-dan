#!/bin/bash

# Try to find ASM jars in the environment
ASM_JARS=$(find /opt/android-sdk -name "asm-9*.jar" -o -name "asm-analysis-9*.jar" -o -name "asm-commons-9*.jar" -o -name "asm-tree-9*.jar" | tr '\n' ':')

if [ -z "$ASM_JARS" ]; then
    # Fallback to original hardcoded paths if not found
    ASM_HOME=$HOME/Downloads/asm-master
    ASM_JARS=$ASM_HOME/asm/build/libs/asm-9.10-SNAPSHOT.jar:$ASM_HOME/asm-analysis/build/libs/asm-analysis-9.10-SNAPSHOT.jar:$ASM_HOME/asm-commons/build/libs/asm-commons-9.10-SNAPSHOT.jar:$ASM_HOME/asm-tree/build/libs/asm-tree-9.10-SNAPSHOT.jar:$ASM_HOME/asm-util/build/libs/asm-util-9.10-SNAPSHOT.jar
fi

PROJECT_HOME=$(pwd)

cd $PROJECT_HOME/bin
# Clear previous outputs, if any
rm -rf out
mkdir out

# Compile all source files, placing them in the bin directory
javac -g -cp .:$PROJECT_HOME/lib/jakarta.servlet-api-6.0.0.jar:$ASM_JARS -d . $(find $PROJECT_HOME/src -name "*.java")

# Copy compiled classes to the 'out' directory for transformation
cp -r app/ odb/ pack/ image.png out/

# Exécuter Parser6 pour chaque classe spécifiée
for class in "$@"; do
    java -cp $ASM_JARS:.:$PROJECT_HOME/lib/jakarta.servlet-api-6.0.0.jar pack.Parser6 $class
done

cd $PROJECT_HOME
echo "---------------"
