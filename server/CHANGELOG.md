# Release Notes and List of Changes

## Version 0.0.4 (2026-09-09)

- One request type for the camera loop, see [docs/loop-architecture.md](../docs/loop-architecture.md).
  The camera passes its status as `cam-status-*` headers on `POST /images/{filename}`. An empty body
  (`Content-Length: 0`) means "status only" - nothing is stored, the settings are returned as usual.
  New `CameraStatus.fromHeaders(HttpHeaders)`, tolerant of missing headers.
- `PUT /status` removed - it is replaced by the headers above.
- `POST /images/{filename}` now passes the camera init counter reported by the camera to the settings
  update instead of a hard-coded `1`, so a rebooted camera receives its camera settings again.
- `WorkflowSettings.delayMs` split into `delayMsActive` (default 20000) and `delayMsPaused` (default 60000).

## Version 0.0.3 (2026-09-04)

- Upgrade to spring-boot 4.1.1 and Java 25

## Version 0.0.2 (2026-09-04)

- Upgrade to imaging-kit 2.1.0
- Upgrade to spring-boot 3.5.16

## Version 0.0.1 (2024-02-14)

- Initial version