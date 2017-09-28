FROM java:8-alpine
MAINTAINER John Flinchbaugh <john@hjsoft.com>

ADD target/uberjar/timeline.jar /timeline/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/timeline/app.jar"]
