FROM ubuntu:precise

MAINTAINER Michael de Jong "https://github.com/michaeldejong"

RUN apt-get update
RUN apt-get -y upgrade

RUN apt-get -y install python-software-properties
RUN add-apt-repository ppa:webupd8team/java
RUN add-apt-repository ppa:natecarlson/maven3
RUN apt-get update && apt-get -y upgrade

# Automatically accept oracle license
RUN echo oracle-java7-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections

# Install java 7 oracle jdk
RUN apt-get -y install oracle-java7-installer && apt-get clean
RUN update-alternatives --display java 
ENV JAVA_HOME /usr/lib/jvm/java-7-oracle

# Install git and maven3
RUN apt-get -y install git
RUN apt-get -y install maven3
RUN ln -s /usr/share/maven3/bin/mvn /usr/bin/mvn

RUN apt-get -y install psmisc xfonts-100dpi xfonts-75dpi xfonts-scalable xfonts-cyrillic xvfb libxrender-dev libxtst6 libxi6

RUN echo '#!/bin/bash' >> /usr/bin/with-xvfb
RUN echo '/usr/bin/Xvfb :99 -ac -screen 0 1024x768x8&' >> /usr/bin/with-xvfb
RUN echo 'sleep 1' >> /usr/bin/with-xvfb
RUN echo '$@' >> /usr/bin/with-xvfb
RUN echo 'exit_value=$?' >> /usr/bin/with-xvfb
RUN echo 'killall Xvfb' >> /usr/bin/with-xvfb
RUN echo 'exit $exit_value' >> /usr/bin/with-xvfb
RUN chmod +x /usr/bin/with-xvfb

ENV DISPLAY :99

RUN echo '#!/bin/bash' >> /usr/bin/prepare
RUN echo 'git clone https://github.com/SERG-Delft/jpacman-framework.git' >> /usr/bin/prepare
RUN echo 'mvn -f jpacman-framework/pom.xml package' >> /usr/bin/prepare
RUN echo 'rm -rf jpacman-framework' >> /usr/bin/prepare
RUN chmod +x /usr/bin/prepare

RUN bash /usr/bin/prepare
RUN rm -rf /usr/bin/prepare


