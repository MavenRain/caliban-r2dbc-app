FROM openjdk:8u281

ARG SBT_VERSION=1.5.4

# Install sbt
RUN \
  mkdir /working/ && \
  cd /working/ && \
  curl -L -o sbt-$SBT_VERSION.deb https://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb && \
  apt-get update && \
  apt-get install sbt && \
  cd && \
  rm -r /working/ && \
  sbt sbtVersion

RUN mkdir -p /root/build/project
COPY . /root/build
RUN cd /root/build && sbt compile

EXPOSE 8088
WORKDIR /root/build

CMD sbt compile run