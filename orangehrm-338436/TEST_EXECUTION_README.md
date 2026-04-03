# OrangeHRM Cucumber Test Execution Guide

## Overview

The Selenium WebDriver test suite supports two execution modes:

| Mode | Browser Window | Use Case | Default? |
|------|---------------|----------|----------|
| **Visible (non-headless)** | ✅ Opens on screen | Local debugging, watching tests run | **Yes** |
| **Headless** | ❌ No window | CI/CD pipelines, servers without display | No |

> **Important:** `xvfb-run` is **not required**. The tests are configured to run
> Chrome (or Firefox) directly. In visible mode, Chrome opens a real browser
> window on your screen. In headless mode, Chrome runs internally without any
> display.

---

## Run Commands

### Visible Mode (default – see the browser UI)

```bash
# Default: browser opens on screen, you can watch login and other actions
mvn test

# Explicitly set non-headless (same as default)
mvn test -Dheadless=false
```

### Headless Mode (for CI/CD)

```bash
# Using system property
mvn test -Dheadless=true

# Using Maven profile
mvn test -Pheadless
```

### Browser Selection

```bash
# Chrome (default)
mvn test -Dbrowser=chrome

# Firefox
mvn test -Dbrowser=firefox
mvn test -Pfirefox

# Firefox headless
mvn test -Pfirefox -Dheadless=true
```

### Tag Filtering

```bash
# Run only smoke tests
mvn test -Dcucumber.filter.tags="@smoke"

# Run smoke tests in headless mode
mvn test -Dcucumber.filter.tags="@smoke" -Dheadless=true
```

---

## Configuration

Configuration is resolved in this priority order:

1. **System properties** (`-Dheadless=true`) — highest priority
2. **Environment variables** (`export headless=true`)
3. **`config.properties`** file (`src/test/resources/config.properties`)
4. **Defaults** in `TestConfig.java`

### Key Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `ORANGEHRM_BASE_URL` | `http://localhost:8080` | Base URL of the OrangeHRM app |
| `ORANGEHRM_USERNAME` | `Admin` | Login username |
| `ORANGEHRM_PASSWORD` | `admin123` | Login password |
| `browser` | `chrome` | Browser type (`chrome` or `firefox`) |
| `headless` | `false` | Headless mode toggle (`true` or `false`) |

### Environment Variable Override

```bash
export headless=true
export browser=firefox
export ORANGEHRM_BASE_URL=http://my-server:8080
mvn test
```

---

## Requirements for Visible Mode

To run tests in visible (non-headless) mode, you need:

- **A display/desktop session** (Linux: X11/Wayland with `DISPLAY` env var set; macOS/Windows: automatic)
- **Google Chrome** (or Chromium) installed
- **No xvfb-run needed** — Chrome runs directly on your display

The driver will log the current mode and DISPLAY status at startup:

```
=== WebDriver Configuration ===
  Browser : chrome
  Headless: false
  DISPLAY : :0
  Mode    : VISIBLE – Chrome will open on screen at DISPLAY=:0
================================
```

If no display is detected, you'll see a warning:

```
  WARNING: Running in visible (non-headless) mode but no DISPLAY
  environment variable is set. The browser may fail to launch.
  TIP: If you are on a headless server or CI, run with -Dheadless=true
  or use the -Pheadless Maven profile.
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Chrome won't launch in visible mode | Ensure `DISPLAY` is set (Linux) or you have a desktop session |
| Tests fail on CI server | Use `mvn test -Pheadless` or `mvn test -Dheadless=true` |
| Wrong browser opens | Check `-Dbrowser=chrome` or `config.properties` |
| Chrome crashes with sandbox errors | `--no-sandbox` is already configured in `DriverFactory` |
