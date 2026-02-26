#!/bin/bash

# Try to find ASM jars in the environment
ASM_JARS=$(find /opt/android-sdk /usr/share/gradle /usr/share/maven -name "asm-9*.jar" -o -name "asm-analysis-9*.jar" -o -name "asm-commons-9*.jar" -o -name "asm-tree-9*.jar" 2>/dev/null | tr '\n' ':')

PROJECT_HOME=$(pwd)

mkdir -p $PROJECT_HOME/bin/out

cd $PROJECT_HOME/bin
# Clear previous outputs, if any
rm -rf out/*

# Compile all source files
javac -g -cp .:$PROJECT_HOME/lib/jakarta.servlet-api-6.0.0.jar:$ASM_JARS -d . $(find $PROJECT_HOME/src -name "*.java")

# Copy compiled classes to the 'out' directory for transformation
cp -r app/ odb/ pack/ out/
# Copy test image if it exists
[ -f $PROJECT_HOME/image.png ] && cp $PROJECT_HOME/image.png out/

# Execute Parser6 for each specified class
for class in "$@"; do
    java -cp $ASM_JARS:.:$PROJECT_HOME/lib/jakarta.servlet-api-6.0.0.jar pack.Parser6 $class
done

cd $PROJECT_HOME
echo "---------------"
