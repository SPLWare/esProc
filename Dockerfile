FROM openjdk:8-jdk-alpine
WORKDIR /app
COPY bin /app/bin
COPY config /app/config
COPY importlibs /app/importlibs
COPY jdbc /app/jdbc
COPY lib /app/lib
COPY mainPath /app/mainPath
COPY target /app/target
# Set executable permission for esprocx.sh
RUN chmod +x /app/bin/ServerConsole2.sh
ENTRYPOINT ["/bin/sh", "/app/bin/ServerConsole2.sh", "-h"]
