#!/bin/bash
# 本地开发启动脚本

# 加载环境变量
export $(cat .env.local | grep -v '^#' | xargs)

# 运行应用
mvn spring-boot:run -pl unimarket-web -am -Dspring-boot.run.profiles=dev