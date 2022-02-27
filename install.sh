#!/usr/bin/env bash


FEDERATE_DIR=$pwd
function show_usage (){
    printf "Usage: $0 customer\n"
    printf "\n"
return 0
}

# if less than three arguments supplied, display usage
	if [  $# -le 0 ]
	then
		show_usage
		exit 1
	fi

CUSTOMER=$1

cp ./run.sh ./customers/$CUSTOMER/run.sh
cp ./build/libs/federate*.jar ./customers/$CUSTOMER/
cd customers/$CUSTOMER
tar -cvf federate.zip federate*.jar *.jks *.sh

rm federate*.jar run.sh

cd $FEDERATE_DIR