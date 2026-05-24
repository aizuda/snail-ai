# 贝尔实验室 Spring 官方推荐镜像, 能解决arm芯片国产操作系统运行问题
FROM bellsoft/liberica-openjdk-debian:21-cds

LABEL maintainer="opensnail"
LABEL app-name="snail-ai-server"
LABEL description="Snail AI is an open-source enterprise AI Agent platform built on Java 21, Spring Boot 4, and Spring AI 2.0, providing a secure, controllable, and scalable distributed architecture for developing and operating intelligent agents."

ENV TZ=Asia/Shanghai LANG=C.UTF-8 LC_ALL=C.UTF-8

WORKDIR /snailai/server
ADD ./target/snail-ai-server-exec.jar app.jar

# 对外暴漏的端口号
EXPOSE 8080
# 数据卷, 用于映射日志目录
VOLUME /snailai/server/data/log
VOLUME /snailai/server/upload

# https://docs.docker.com/reference/build-checks/json-args-recommended/
CMD [ "java", "-Djava.security.egd=file:/dev/./urandom", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:+UseZGC", "-jar", "app.jar" ]