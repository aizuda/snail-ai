FROM amazoncorretto:17
LABEL maintainer="opensnail"

ADD ./target/snail-ai-agent-example.jar snail-ai-agent-example.jar

#对外暴漏的端口号
EXPOSE 8090

WORKDIR /

#开机启动
ENTRYPOINT ["sh","-c","java -jar $JAVA_OPTS /snail-ai-agent-example.jar $PARAMS"]

