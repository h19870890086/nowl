# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# 复制 Maven 镜像配置（阿里云加速）
COPY settings.local.xml /root/.m2/settings.xml

# 复制全部源代码
COPY . .

# 直接构建（跳过 dependency:go-offline，避免 SSL 问题阻塞构建）
RUN mvn clean package -Dmaven.test.skip=true -pl unimarket-web -am -B

# 运行阶段
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 复制jar包
COPY --from=builder /app/unimarket-web/target/*.jar app.jar

# 设置时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "-Xms512m", "-Xmx1024m", "-Dfile.encoding=UTF-8", "app.jar"]
