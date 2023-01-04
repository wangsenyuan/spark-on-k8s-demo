ARG SPARK_IMAGE=registry.cn-hangzhou.aliyuncs.com/fesco/spark:v3.4.0
FROM ${SPARK_IMAGE}
ARG spark_uid=185
# Switch to user root so we can add additional jars, packages and configuration files.
# Add project files
USER root

RUN set -ex && \
    sed -i 's/http:\/\/deb.\(.*\)/https:\/\/deb.\1/g' /etc/apt/sources.list && \
    apt-get --allow-releaseinfo-change update && \
    apt install -y bash tini libc6 libpam-modules krb5-user libnss3 procps unzip && \
    ls -l /opt/spark/jars/zstd-jni-1.5.2-5.jar && \
    unzip /opt/spark/jars/zstd-jni-1.5.2-5.jar -d /tmp  && \
    cp /tmp/linux/amd64/libzstd-jni-1.5.2-5.so /lib && \
    mkdir -p /app/data && \
    mkdir -p /opt/spark/conf && \
    chmod a+x /app/data && \
    echo "auth required pam_wheel.so use_uid" >> /etc/pam.d/su && \
    chgrp root /etc/passwd && chmod ug+rw /etc/passwd

# ADD lib/postgresql-42.5.1.jar /opt/lib/
ADD target/scala-2.12/spark-sql-demo.jar /opt/lib/

RUN chmod a+x /opt/decom.sh && \
    chmod a+x /opt/entrypoint.sh && \
    chmod -R 777 /tmp

ENTRYPOINT [ "/opt/entrypoint.sh" ]

# Specify the User that the actual main process will run as
# USER ${spark_uid}
