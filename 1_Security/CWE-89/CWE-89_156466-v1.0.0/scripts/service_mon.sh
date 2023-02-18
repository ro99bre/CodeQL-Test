#!/bin/bash
IS_UP=1
until [ $IS_UP -eq 0 ]; do
	(echo > /dev/tcp/$1/$2) &> /dev/null
	IS_UP=$?
done
