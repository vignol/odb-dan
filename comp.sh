#!/bin/bash

ASM_HOME=$HOME/Downloads/asm-master
ASM_JARS=/opt/android-sdk/cmdline-tools/latest/lib/external/org/ow2/asm/asm/9.7/asm-9.7.jar:/opt/android-sdk/cmdline-tools/latest/lib/external/org/ow2/asm/asm-analysis/9.7/asm-analysis-9.7.jar:/opt/android-sdk/cmdline-tools/latest/lib/external/org/ow2/asm/asm-commons/9.7/asm-commons-9.7.jar:/opt/android-sdk/cmdline-tools/latest/lib/external/org/ow2/asm/asm-tree/9.7/asm-tree-9.7.jar:
PROJECT_HOME=$(pwd)

cd $PROJECT_HOME/bin
# Clear previous outputs, if any
rm -rf app odb pack out
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
