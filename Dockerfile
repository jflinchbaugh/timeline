FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/timeline.jar /timeline/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/timeline/app.jar"]
