# Multi Format Mock Server

## Building & Running

To build or run the project, use one of the following tasks:

| Task              | Description      |
|-------------------|------------------|
| `./gradlew test`  | Run the tests    |
| `./gradlew build` | Build everything |
| `./gradlew run`   | Run application  |

To run the application, you can also call `main` from [
`Application.kt`](./src/main/kotlin/cz/cvut/fit/atlasest/application/Application.kt)

## Collections

The collections are defined in the `db.json` file. The file contains sample collections *books*, *users*, and *loans*. These can be modified or deleted and new collections can be added. 

## Custom identifiers

The default identifier for all collections is `id`. To change the default identifier the `-i`/`-identifier` cli option can be used with the default identifier as value. If a particular collection has a custom identifier this must be defined in the `identifiers.json` file. For example, the sample `users` collection has a custom identifier `identifier` which is defined in this file.

## Pagination limit
The default pagination limit is `10`. To change this add the `-l`/`-limit` cli option with the new default limit as value.

## Demo application
To showcase the features of this mock server a [demo application](https://gitlab.fit.cvut.cz/atlasest/library-management-app) was created. This application expectes the data to stay in the default format as they are in `db.json`. However, the number of items in each collection can be modified.

## JSON schema

Use JSON schema from file by adding the `-s`/`-schema` cli option with the filename as value (currently there is only support for [Draft 2020-12](https://json-schema.org/draft/2020-12#draft-2020-12)).

If no schema file will be provided, the JSON schema will be inferred from the collections.

## OpenAPI specification

When the server is running, the generated OpenAPI specification will be available at `/$rootPath/openapi.json` and
SwaggerUI will be available at `/$rootPath/swagger-ui`.

## Docker
 The [`CI/CD pipeline`](.gitlab-ci.yml) automatically builds and pushed the image to Docker Hub. The image is available [here](https://hub.docker.com/repository/docker/atlasest/multi-format-mock-server/general). To use Docker, make sure you have it installed. You can follow the official installation guides based on your operating system:

* **Linux**: https://docs.docker.com/engine/install/
* **Windows**: https://docs.docker.com/desktop/setup/install/windows-install/
* **MacOS**: https://docs.docker.com/desktop/setup/install/mac-install/
* An alternative for Linux and MacOS is [Colima](https://github.com/abiosoft/colima)

### Pull image from Docker Hub
To pull image from Docker Hub and run as container run the following commands:

| Command                                                     | Description                    |
|-------------------------------------------------------------|--------------------------------|
| `docker pull atlasest/multi-format-mock-server`             | Pull the image from Docker Hub |
| `docker run -p 8080:8080 atlasest/multi-format-mock-server` | Run container                  |

### Building and running Docker image locally

To build and run the Docker image locally use the following commands:

| Command                                            | Description            |
|----------------------------------------------------|------------------------|
| `docker build -t multi-format-mock-server .`       | Build the Docker image |
| `docker run -p 8080:8080 multi-format-mock-server` | Run container          |
 
### Mounting local files to Docker container
The Docker image includes some example data and default configuration. To mount local files (
[`db.json`](db.json), [`identifiers.json`](identifiers.json), [`application.yaml`](src/main/resources/application.yaml)) when running container add the --volume flag.

`--volume <volume-name>:<mount-path>`

For example with `db.json`:

`--volume ./db.json:/app/db.json`

### Running Docker container with custom JSON schema

To run the container with a custom schema the schema file must be mounted and the container must be run with the `-s` flag.

`docker run -p 8080:8080  --volume ./schema.json:/app/schema.json <image-tag> -s schema.json`

### Docker Compose

The repository includes [docker-compose.yml](docker-compose.yml). This file can be used to run the application's Docker image. The file also includes commented-out volume mounting as well as the cli options for custom identifiers, JSON Schema and default pagination limit, to use these just uncomment them and edit as needed. To get started, make sure you have Docker Compose installed ([installation guide](https://docs.docker.com/compose/install/)), then run:

```shell
    docker-compose up
```

### Note for MacOS Sequoia 15.2 users:

When building the image locally on MacOS Sequoia 15.2 there is a [bug](https://bugs.openjdk.org/browse/JDK-8345296) in JVM, for which the workaround is to add `ENV JAVA_TOOL_OPTIONS="-XX:UseSVE=0"` to **each stage** in the Dockerfile. This disables SVE usage in the JVM. A more detailed explanation can be found ([here](https://medium.com/@luketn/java-on-docker-sigill-exception-on-mac-os-sequoia-15-2-9311e4775442)). The Dockerfile already includes this fix as commented-out lines—you can simply uncomment them if needed.

### Inspiration

Inspired by the API design of [JSON Server](https://github.com/typicode/json-server).
