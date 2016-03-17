#!/bin/bash

# get java
wget --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u74-b02/jdk-8u74-linux-x64.tar.gz -nv

tar xzf jdk-8u74-linux-x64.tar.gz
rm jdk-8u74-linux-x64.tar.gz

echo "export JAVA_HOME=~/jdk1.8.0_74" >> /home/vagrant/.bashrc
echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> /home/vagrant/.bashrc

# get lein
mkdir bin
cd bin
wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein -nv
chmod +x lein
echo "export PATH=~/bin:\$PATH" >> /home/vagrant/.bashrc

# get the code
cd /home/vagrant
apt-get install git -y
git clone https://github.com/dpzmick/auto_parallel.git
