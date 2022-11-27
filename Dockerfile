FROM openjdk:19

ENV SBT_VERSION 1.6.2

RUN curl -L -o sbt-$SBT_VERSION.tgz https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.tgz
RUN tar -xf sbt-$SBT_VERSION.tgz

WORKDIR /app

ADD . /app

CMD /sbt/bin/sbt "runMain RunAllSimulations"