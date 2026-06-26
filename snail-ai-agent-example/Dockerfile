FROM bellsoft/liberica-openjdk-debian:21-cds
LABEL maintainer="opensnail"

ENV TZ=Asia/Shanghai LANG=C.UTF-8 LC_ALL=C.UTF-8

ADD ./target/snail-ai-agent-example.jar snail-ai-agent-example.jar

WORKDIR /snailai/agent

#对外暴漏的端口号
EXPOSE 8081

# 数据卷, 用于映射日志目录
VOLUME /snailai/agent/data/log

#开机启动
ENTRYPOINT ["sh","-c","java -jar $JAVA_OPTS /snail-ai-agent-example.jar $PARAMS"]

