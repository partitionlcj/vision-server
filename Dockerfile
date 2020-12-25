FROM xharbor.i-tetris.com:5000/public/centos7-with-tools:v1.0

# mount jdk path
VOLUME /opt/java/jdk-1.8

RUN mkdir -p /data/vision-server-image/storImage/
RUN mkdir -p /data/vision-server-image/deleteImage/
VOLUME /data/vision-server-image/storImage/
VOLUME /data/vision-server-image/deleteImage/

ENV JAVA_HOME /opt/java/jdk-1.8
ENV PATH $PATH:${JAVA_HOME}/jre/bin:${JAVA_HOME}/bin

ENV LANG=zh_CN.UTF-8 \
    LANGUAGE=zh_CN:zh \
    LC_ALL=zh_CN.UTF-8

# Install tools
RUN yum update -y && \
    yum reinstall -y glibc-common && \
    yum install -y telnet net-tools && \
    yum clean all && \
    rm -rf /tmp/* rm -rf /var/cache/yum/* && \
    localedef -c -f UTF-8 -i zh_CN zh_CN.UTF-8 && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

ENV USER_DIR /data/app/vision-server

RUN yum -y install unzip
RUN yum -y install libgomp

RUN mkdir -p $USER_DIR
WORKDIR $USER_DIR

COPY target/vision-server-1.0-SNAPSHOT.jar ./
#RUN unzip vision-server-1.0-SNAPSHOT.jar

EXPOSE 10099

#ENTRYPOINT ["sleep", "60"]
ENTRYPOINT ["sh", "./bin/start.sh"]
