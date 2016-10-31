@echo off
mkdir out\classes
javac src/com/gbjl/strips/*.java src/com/gbjl/strips/examples/thecoffeeserver/*.java -d out/classes
cd out/classes
jar cfe ../thecoffeeserver.jar com.gbjl.strips.examples.thecoffeeserver.Main com/gbjl/strips/*.class com/gbjl/strips/examples/thecoffeeserver/*.class
cd ../..
