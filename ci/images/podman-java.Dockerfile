FROM quay.io/podman/stable:v4.7

# install and configure java
RUN dnf -y install java-21-openjdk-headless java-21-openjdk-devel fontconfig containernetworking-cni && dnf -y clean all
ENV JAVA_HOME /usr/lib/jvm/jre

# add podman start script
RUN echo -en "#!/bin/bash\npodman system service -t 0 unix:///tmp/docker.sock &" > /usr/bin/start-podman \
 && chmod +x /usr/bin/start-podman

USER podman

# set podman user settings
RUN mkdir -p $HOME/.config/containers
RUN echo -en "[containers]\nnetns=\"slirp4netns\"" > $HOME/.config/containers/containers.conf

# quarkus properties
RUN echo "ryuk.container.privileged=true" > $HOME/.testcontainers.properties
# ENV TESTCONTAINERS_RYUK_DISABLED=true
ENV TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="/tmp/docker.sock"
ENV DOCKER_HOST="unix:///tmp/docker.sock"
