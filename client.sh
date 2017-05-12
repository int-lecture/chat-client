#!/bin/sh
java -cp lib/jersey-client-1.19.1.jar:lib/jersey-core-1.19.1.jar:lib/jsr311-api-1.1.1.jar:bin/:lib/json-20160810.jar var.chatclient.ChatClient http://localhost:5000 $1 $2

