# ==========================================
# 慧停管家 · 后端 Java Spring Boot Dockerfile
# ==========================================
FROM docker.1panel.live/library/openjdk:17-jdk-slim

WORKDIR /app

# 复制已预编译完成的可执行 Spring Boot Jar 包
COPY target/parking-java-1.0.0.jar /app/app.jar

# 持久化数据目录
RUN mkdir -p /app/data

# 暴露端口
EXPOSE 8080

# 环境变量配置
ENV SERVER_PORT=8080

# 启动 Spring Boot 应用
CMD ["java", "-Dfile.encoding=UTF-8", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
