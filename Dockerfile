# Build the application using Java 17 and Maven
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn -B clean package

# Run the WAR using Tomcat 10.1
FROM tomcat:10.1.62-jdk17-temurin

# Deploy SpendWise as the root application
COPY --from=build /app/target/SpendWise.war /usr/local/tomcat/webapps/ROOT.war

# Render's default web service port
ENV PORT=10000

EXPOSE 10000

# Configure Tomcat to listen on Render's port
CMD ["sh", "-c", "sed -i 's/port=\"8080\"/port=\"'\"${PORT:-10000}\"'\"/' /usr/local/tomcat/conf/server.xml && catalina.sh run"]
