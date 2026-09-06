# Release Notes and List of Changes

## Version 0.0.4 (2026-09-06)

- New view "Camera Status" (route `camera-status`), reachable from the drawer menu. It shows one table per
  camera with all attributes of a camera status record (timestamp, RSSI, image counter, image errors, camera
  init counter, camera init errors, upload image errors, upload status errors), ordered by timestamp in
  descending order. A "Reload" button refetches the data.
- New model record `CameraStatusRecord`, mirroring the record of the server module
- `FileViewService` extended by `listCameras()` and `listCameraStatus(String)`, calling the backend endpoints
  `cameras` and `status/{cameraName}`. The camera names are read with a `ParameterizedTypeReference`, because
  `bodyToFlux(String.class)` is served by the raw `StringDecoder` and would not decode the JSON array.
- New tests: `CameraStatusRecordTest`, `CameraStatusViewTest` and `FileViewServiceTest` (the latter against a
  local HTTP test double, so the JSON decoding is covered too); `RouteAccessTest` extended by the new view

## Version 0.0.3 (2026-09-04)

- Upgrade to Vaadin 25.2.6, spring-boot 4.1.1
- `SecurityConfig` migrated from the removed `VaadinWebSecurity` base class to a `SecurityFilterChain`
  bean using `VaadinSecurityConfigurer`
- `AntPathRequestMatcher` replaced by `PathPatternRequestMatcher` (removed in Spring Security 7)
- `vaadin.whitelisted-packages` renamed to `vaadin.allowed-packages`
- `lumoImports` removed from `theme.json` (unsupported in Vaadin 25); the Lumo utility classes used by
  `MainLayout` are now loaded via `@StyleSheet(Lumo.UTILITY_STYLESHEET)`
- Obsolete `frontend/index.html` removed so Vaadin generates the current default
- Test bench: deprecated `first()` replaced by `single()`
- `MainLayout` annotated with `@PermitAll`: Vaadin 25 also enforces the access rules of parent layouts,
  so navigation failed after login with "Denied access to view 'ImagesView' due to parent layout
  'MainLayout' access rules"
- New `RouteAccessTest` guarding the access annotations of the views and their parent layout
- `SecurityConfig` allows the static resources of the views (`/components/**`, `/js/**`) for authenticated
  users: Vaadin 25 derives "anyRequest" from the Flow route registry, so these non-route resources were
  answered with 403 and the image/video viewer iframes stayed empty
- New `StaticResourceAccessTest` verifying these resources over real HTTP

## Version 0.0.2 (2026-09-04)

- Upgrade to spring-boot 3.5.16 and Java 25

## Version 0.0.1 (2024-02-14)

- Initial version