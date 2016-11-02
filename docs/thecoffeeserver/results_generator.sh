#!/bin/sh

i="0"

while [ $i -lt $1 ]
do
	PETITIONS=$(( ( RANDOM % 18 )  + 1 ))
	MACHINES=$(( ( RANDOM % 18 )  + 1 ))
	python case_generator.py $PETITIONS $MACHINES > ../../out/temporal_random_case.txt
	java -jar ../../out/thecoffeeserver.jar ../../out/temporal_random_case.txt ../../out/temporal_random_result.txt

	if [ $? -eq 0 ]
	then
		STEPS=`cat ../../out/temporal_random_result.txt | grep -a -o 'Steps([0-9]*)' | tail -n 1 | grep -o '[0-9]*'`
		echo $PETITIONS,$MACHINES,$STEPS
		i=$[$i+1]
	fi
done
