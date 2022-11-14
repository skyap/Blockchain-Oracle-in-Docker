FROM ubuntu:20.04

RUN apt-get update -y
RUN apt install git -y
RUN DEBIAN_FRONTEND="noninteractive" apt-get -y install tzdata
RUN apt-get install cmake curl sudo nano screen zip unzip wget -y
RUN curl -fsSL https://deb.nodesource.com/setup_16.x | sudo -E bash -
RUN sudo apt-get install -y nodejs
RUN npm install -g ganache-cli
RUN npm install -g solc
RUN apt-get install openjdk-8-jdk -y
RUN curl -L get.web3j.io | sh

COPY Java /home/Java
COPY Smart_contracts /home/Smart_contracts

RUN cd /home/Smart_contracts && solcjs --abi --bin --optimize *.sol -o .

RUN cd /home/Smart_contracts && /root/.web3j/web3j generate solidity -a Oracle_sol_Oracle.abi -b Oracle_sol_Oracle.bin -o . -p OracleProject


RUN cd /home/Smart_contracts && /root/.web3j/web3j generate solidity -a Request_sol_Request.abi -b Request_sol_Request.bin -o . -p OracleProject


RUN cd /home/Smart_contracts && /root/.web3j/web3j generate solidity -a Response_sol_Response.abi -b Response_sol_Response.bin -o . -p OracleProject


RUN cp -r /home/Smart_contracts/OracleProject/* /home/Java/OracleProject

RUN cd /home/Java && javac  -cp .:OracleProject/lib/* OracleProject/*.java -Xlint:unchecked

CMD cd /home/Java && ./start.sh





