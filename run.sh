#!/bin/sh

export COMPOSE_FILE_PATH="${PWD}/target/classes/docker/docker-compose.yml"

if [ -z "${M2_HOME}" ]; then
  export MVN_EXEC="mvn"
else
  export MVN_EXEC="${M2_HOME}/bin/mvn"
fi

start() {
    docker volume create salesforcepolicy-acs-volume
    docker volume create salesforcepolicy-db-volume
    docker volume create salesforcepolicy-ass-volume
    docker-compose -f "$COMPOSE_FILE_PATH" up --build -d
}

start_share() {
    docker-compose -f "$COMPOSE_FILE_PATH" up --build -d salesforcepolicy-share
}

start_acs() {
    docker-compose -f "$COMPOSE_FILE_PATH" up --build -d salesforcepolicy-acs
}

down() {
    if [ -f "$COMPOSE_FILE_PATH" ]; then
        docker-compose -f "$COMPOSE_FILE_PATH" down
    fi
}

purge() {
    docker volume rm -f salesforcepolicy-acs-volume
    docker volume rm -f salesforcepolicy-db-volume
    docker volume rm -f salesforcepolicy-ass-volume
}

build() {
    $MVN_EXEC clean package
}

build_share() {
    docker-compose -f "$COMPOSE_FILE_PATH" kill salesforcepolicy-share
    yes | docker-compose -f "$COMPOSE_FILE_PATH" rm -f salesforcepolicy-share
    $MVN_EXEC clean package -pl salesforcepolicy-share,salesforcepolicy-share-docker
}

build_acs() {
    docker-compose -f "$COMPOSE_FILE_PATH" kill salesforcepolicy-acs
    yes | docker-compose -f "$COMPOSE_FILE_PATH" rm -f salesforcepolicy-acs
    $MVN_EXEC clean package -pl salesforcepolicy-integration-tests,salesforcepolicy-platform,salesforcepolicy-platform-docker
}

tail() {
    docker-compose -f "$COMPOSE_FILE_PATH" logs -f
}

tail_all() {
    docker-compose -f "$COMPOSE_FILE_PATH" logs --tail="all"
}

prepare_test() {
    $MVN_EXEC verify -DskipTests=true -pl salesforcepolicy-platform,salesforcepolicy-integration-tests,salesforcepolicy-platform-docker
}

test() {
    $MVN_EXEC verify -pl salesforcepolicy-platform,salesforcepolicy-integration-tests
}

case "$1" in
  build_start)
    down
    build
    start
    tail
    ;;
  build_start_it_supported)
    down
    build
    prepare_test
    start
    tail
    ;;
  start)
    start
    tail
    ;;
  stop)
    down
    ;;
  purge)
    down
    purge
    ;;
  tail)
    tail
    ;;
  reload_share)
    build_share
    start_share
    tail
    ;;
  reload_acs)
    build_acs
    start_acs
    tail
    ;;
  build_test)
    down
    build
    prepare_test
    start
    test
    tail_all
    down
    ;;
  test)
    test
    ;;
  *)
    echo "Usage: $0 {build_start|build_start_it_supported|start|stop|purge|tail|reload_share|reload_acs|build_test|test}"
esac