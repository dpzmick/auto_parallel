#!/bin/bash

# roughly what needs to happen to set up a new machine
sudo apt-get install git -y
# sudo apt-get install vim -y

# get java
wget --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u74-b02/jdk-8u74-linux-x64.tar.gz -nv

tar xzf jdk-8u74-linux-x64.tar.gz
rm jdk-8u74-linux-x64.tar.gz

echo "export JAVA_HOME=~/jdk1.8.0_74" > ~/.bashrc
echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> ~/.bashrc

# get lein
mkdir bin
cd bin
wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein -nv
chmod +x lein
echo "export PATH=~/bin:\$PATH" >> ~/.bashrc
echo "export LEIN_ROOT=1" >> ~/.bashrc

# get the code
cd
git clone https://github.com/dpzmick/auto_parallel.git
