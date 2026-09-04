# Release Notes and List of Changes

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

## Version 0.0.2 (2026-09-04)

- Upgrade to spring-boot 3.5.16 and Java 25

## Version 0.0.1 (2024-02-14)

- Initial version