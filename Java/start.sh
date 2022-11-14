#!/bin/bash
screen -dmS ganache ganache-cli --deterministic
sleep 5
screen -dmS Main java -cp .:OracleProject/lib/* OracleProject.Main
sleep 5
screen -dmS Request java -cp .:OracleProject/lib/* OracleProject.RequestContractBooter
/bin/bash
