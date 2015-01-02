FROM ubuntu:precise

MAINTAINER Michael de Jong "https://github.com/michaeldejong"

RUN apt-get update
RUN apt-get -y upgrade

RUN apt-get -y install python-software-properties
RUN add-apt-repository ppa:webupd8team/java
RUN add-apt-repository ppa:natecarlson/maven3
RUN apt-get update && apt-get -y upgrade

# Automatically accept oracle license
RUN echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections

# Install java 8 oracle jdk
RUN apt-get -y install oracle-java8-installer && apt-get clean
RUN update-alternatives --display java 
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

# Install git and maven3
RUN apt-get -y install git
RUN apt-get -y install maven3
RUN ln -s /usr/share/maven3/bin/mvn /usr/bin/mvn

RUN apt-get -y install psmisc xfonts-100dpi xfonts-75dpi xfonts-scalable xfonts-cyrillic xvfb libxrender-dev libxtst6 libxi6

RUN echo '#!/bin/bash' >> /usr/bin/with-xvfb
RUN echo '/usr/bin/Xvfb :99 -ac -screen 0 1024x768x8 2>&1 2>/dev/null &' >> /usr/bin/with-xvfb
RUN echo 'sleep 1' >> /usr/bin/with-xvfb
RUN echo '$@' >> /usr/bin/with-xvfb
RUN echo 'exit_value=$?' >> /usr/bin/with-xvfb
RUN echo 'killall Xvfb' >> /usr/bin/with-xvfb
RUN echo 'exit $exit_value' >> /usr/bin/with-xvfb
RUN chmod +x /usr/bin/with-xvfb

RUN echo '#!/bin/bash' >> /usr/bin/fetch-deps
RUN echo 'git clone https://github.com/SERG-Delft/jpacman-framework.git' >> /usr/bin/fetch-deps
RUN echo 'cd jpacman-framework' >> /usr/bin/fetch-deps
RUN echo 'git checkout 5f132d16d9258026ab1a19a973c23f275c1ad0e9' >> /usr/bin/fetch-deps
RUN echo 'with-xvfb mvn -B clean install' >> /usr/bin/fetch-deps
RUN echo 'cd ../' >> /usr/bin/fetch-deps
RUN echo 'rm -rf jpacman-framework' >> /usr/bin/fetch-deps
RUN echo 'git clone https://github.com/SERG-Delft/jpacman-template.git' >> /usr/bin/fetch-deps
RUN echo 'cd jpacman-template' >> /usr/bin/fetch-deps
RUN echo 'with-xvfb mvn -B package' >> /usr/bin/fetch-deps
RUN echo 'cd ../' >> /usr/bin/fetch-deps
RUN echo 'rm -rf jpacman-template' >> /usr/bin/fetch-deps
RUN chmod +x /usr/bin/fetch-deps

ENV DISPLAY :99

RUN fetch-deps
RUN rm -rf /usr/bin/fetch-deps
