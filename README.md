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

The collections are defined in the `db.json` file.

## JSON schema

Use JSON schema from file by adding the `-s` flag with the filename as value

If no schema file will be provided, the JSON schema will be inferred from the collections.

## OpenAPI specification

When the server is running, the generated OpenAPI specification will be available at `/$rootPath/openapi.json` and the
SwaggerUI will be available at `/$rootPath/swagger-ui`.
