# Spring Boot and Vaadin

## Build/Run

- `mvn -DskipTests package`
- `mvn -P production -DskipTests package`
- `java -jar target/cam-recorder-ui.jar`

## TODO

- [x] Image viewer with scaling
- [x] Restart every ... as setting from server (client + server)
- [x] Support for displaying videos
- [x] Create timelapse videos from images using *ffmpeg*
- [x] Lazy loading for handling large amounts of image files
- [ ] Offer UI to configure timelapse video creation
- [x] Limit display to 100 entries. Show total number in label, e.g. "100 of 1000 - use filter to reduce"
- [ ] Possibility to define labels for images
- [ ] Download n images as ZIP
- [ ] Image viewer as WebComponent

## Vaadin Links

- [Vaadin flow/tutorials/in-depth-course](https://vaadin.com/docs/latest/flow/tutorials/in-depth-course)
- [Examples and Demos](https://vaadin.com/examples-and-demos)
- [Built With Vaadin](https://github.com/vaadin/built-with-vaadin)
- [GitHub flow-spring-examples](https://github.com/vaadin/flow-spring-examples)
- [GitHub skeleton-starter-flow-spring](https://github.com/vaadin/skeleton-starter-flow-spring)
- [GitHub flow-crm-tutorial - also Spring Boot](https://github.com/vaadin/flow-crm-tutorial)