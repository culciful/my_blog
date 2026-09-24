# 多阶段构建：第一阶段编译，第二阶段只保留运行时需要的 jar，镜像体积小很多
# （这个项目没带 mvnw，构建阶段直接用带 Maven 的基础镜像）

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
# 先只拷 pom.xml 装依赖，源码没变时这层能复用 Docker layer 缓存，不用每次都重新拉全部依赖
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
# 镜像构建阶段不跑测试——测试要连真实 MySQL（ContractSmokeTest/IntegrationSmokeTest 都是
# 真打接口查库的集成测试，不是纯单测），构建时数据库不一定在，跑不了；测试在 CI / 本地 mvn
# test 里跑，不挂在镜像构建这一步
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# 头像/文章图片落盘在这个目录（UploadProperties.dir 默认 "uploads"，相对进程工作目录）；
# 不挂 volume 的话容器重建一次图片就全没了
RUN mkdir -p /app/uploads
COPY --from=build /build/target/springboot3-blog-*.jar app.jar

EXPOSE 8080
# 激活哪个 profile 不在这里写死——用 SPRING_PROFILES_ACTIVE 环境变量控制（compose 里配）。
# 关键原因：application-prod.yaml 把 jwt.cookie.secure 设成了 true，这个 cookie 只有走 HTTPS
# 浏览器才会真的带上；docker-compose.yml 这套是本地/测试用的纯 HTTP 编排（没有 TLS 终结），
# 硬编码成 prod 会导致登录态在这个 compose 场景下直接失效。真要挂到有 HTTPS 反代的生产环境，
# 到时候把 SPRING_PROFILES_ACTIVE=prod 设到反代那台机器的部署配置里，镜像不用重建。
# 环境变量全部外部化了（JWT_TOKEN_SIGN_KEY / DB_* / BLOG_PUBLIC_ORIGIN / SPRING_MAIL_* 等，
# 见 application.yaml / application-prod.yaml），compose 里注入，镜像本身不含任何密钥。
ENTRYPOINT ["java", "-jar", "app.jar"]
