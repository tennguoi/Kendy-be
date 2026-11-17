# Tận dụng file jar đã được build từ Jenkins host (dùng cache .m2 có sẵn trên máy)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk upgrade --no-cache
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
