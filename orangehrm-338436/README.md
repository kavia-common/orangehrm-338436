# OrangeHRM Cucumber BDD Test Suite

Cucumber BDD (Behavior-Driven Development) UI automation test suite for the OrangeHRM web application, built with **Selenium WebDriver** and **JUnit 5**.

## Technology Stack

| Component          | Technology                      | Version  |
|--------------------|---------------------------------|----------|
| BDD Framework      | Cucumber                        | 7.15.0   |
| Test Framework     | JUnit 5 (Jupiter)               | 5.10.1   |
| Browser Automation | Selenium WebDriver              | 4.17.0   |
| Driver Management  | WebDriverManager                | 5.6.3    |
| Build Tool         | Apache Maven                    | 3.8+     |
| Java               | OpenJDK                         | 17       |

## Project Structure

```
src/test/
├── java/com/orangehrm/
│   ├── config/
│   │   └── TestConfig.java          # Centralized test configuration
│   ├── driver/
│   │   └── DriverFactory.java       # WebDriver lifecycle management
│   ├── pages/
│   │   └── LoginPage.java           # Page Object for login page
│   ├── runner/
│   │   └── CucumberTestRunner.java  # JUnit 5 Cucumber test runner
│   └── stepdefinitions/
│       ├── Hooks.java               # Before/After scenario hooks
│       └── LoginStepDefinitions.java # Login feature step implementations
└── resources/
    ├── features/
    │   └── login.feature            # Login BDD scenarios
    ├── config.properties            # Test configuration defaults
    └── junit-platform.properties    # Cucumber/JUnit platform config
```

## Prerequisites

- **Java 17** or higher
- **Apache Maven 3.8+**
- **Chrome** or **Firefox** browser installed
- OrangeHRM application running and accessible

## Quick Start

### Run all tests
```bash
mvn test
```

### Run tests with a specific tag
```bash
mvn test -Dcucumber.filter.tags="@smoke"
```

### Run in headless mode (CI)
```bash
mvn test -Pheadless
```

### Run with Firefox
```bash
mvn test -Pfirefox
```

### Specify a custom base URL
```bash
mvn test -DORANGEHRM_BASE_URL=http://your-orangehrm-server:8080
```

## Configuration

Configuration can be set via (in priority order):
1. **System properties** (`-Dkey=value` on the command line)
2. **Environment variables** (e.g., `ORANGEHRM_BASE_URL`)
3. **config.properties** file (`src/test/resources/config.properties`)

| Property                | Default             | Description                          |
|-------------------------|---------------------|--------------------------------------|
| `ORANGEHRM_BASE_URL`   | `http://localhost:8080` | Base URL of the OrangeHRM app    |
| `ORANGEHRM_USERNAME`   | `Admin`             | Default login username               |
| `ORANGEHRM_PASSWORD`   | `admin123`          | Default login password               |
| `browser`              | `chrome`            | Browser: `chrome` or `firefox`       |
| `headless`             | `false`             | Run in headless mode                 |

## Reports

After test execution, reports are generated in:
- **HTML Report**: `target/cucumber-reports/cucumber-report.html`
- **JSON Report**: `target/cucumber-reports/cucumber-report.json`
- **Console Output**: Pretty-printed scenario results

## Adding New Tests

1. Create a `.feature` file in `src/test/resources/features/`
2. Add step definitions in `src/test/java/com/orangehrm/stepdefinitions/`
3. Create Page Objects in `src/test/java/com/orangehrm/pages/` as needed
4. Run with `mvn test` to verify
