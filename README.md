# census-rm-exception-manager
This service provides a read-only API for case details.

## How to run
Build it using `mvn clean install` and then execute the JAR file or run in your favourite debugger.

## Workaround | Build with Dockerized Maven

The Maven installation on my MAC was the wrong version so I ran the maven install within Docker.

```
docker run -it --rm --name vm.maven.install -v $PWD:/usr/src/mymaven -w /usr/src/mymaven maven:3.6.0-jdk-11 mvn clean install
```

This creates the JAR file necessary for copying into the Docker image built by the Dockerfile.

```
docker build --tag eu.gcr.io/census-rm-ci/rm/census-rm-exception-manager .
docker image ls
cd src/test/resources/
docker-compose up -d
# Now you can test against the dockerizedd database and queue machines
```

## How to test
make test
