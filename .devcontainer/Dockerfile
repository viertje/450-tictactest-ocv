# Zulu specifically: gradle/gradle-daemon-jvm.properties pins toolchainVendor=AZUL,
# and this image satisfies it, so Gradle provisions no JDK of its own. On musl that
# matters -- a miss falls back to that file's glibc builds, which cannot run here.
FROM azul/zulu-openjdk-alpine:25

# Links the image to the repository in the GitHub Container Registry. An image
# pushed by hand is NOT linked automatically (only images pushed by a workflow are),
# and without that link the workflow's GITHUB_TOKEN may not pull the private package.
LABEL org.opencontainers.image.source=https://github.com/viertje/450-tictactest-ocv
LABEL org.opencontainers.image.description="Build and test environment for TicTacTest: JDK 25 (Zulu) on Alpine, used by the CI pipeline and as a devcontainer."

# bash is for the interactive shell only; gradlew itself is POSIX sh.
RUN apk add --no-cache bash ca-certificates curl git

# 1000:1000 is the usual first-user id on Linux hosts, so when the host user is
# 1000 bind-mounted files stay owned by them rather than root. For any other host
# uid the devcontainer CLI rewrites these on create; both ids are free in the image.
RUN addgroup -g 1000 dev \
 && adduser -u 1000 -G dev -s /bin/bash -D dev \
 && mkdir -p /home/dev/.gradle \
 && chown -R 1000:1000 /home/dev

# Creating .gradle above seeds the volume mounted over it with this ownership.
ENV GRADLE_USER_HOME=/home/dev/.gradle \
    LANG=C.UTF-8

# Default user for interactive/devcontainer use.
# GitHub Actions creates the job workspace (/__w) as root, so a container job that
# runs as "dev" fails with "permission denied" on checkout. The workflow therefore
# overrides this with:
#   container:
#     options: --user root
USER dev