# Playwright Automation Project

## How to Run This Project

1. **Clone the repository:**
    ```bash
    git clone https://github.com/lukachrk/Energo-pro-automationTS.git
    cd Playwright-automationTS
    ```

2. **Install dependencies:**
    ```bash
    npm install
    npx playwright install
    ```

3. **Create .env file with your username and password:**
    ```env
    USER=<your-username>
    PASSWORD=<your-password>
    ```


4. **Run the tests:**
    ```bash
    npx playwright test
    ```

5. **Generate and view the test report:**
    ```bash
    npx playwright show-report
    ```

6. **Run tests with a specific browser:**
    ```bash
    npx playwright test --project=chromium
    npx playwright test --project=firefox
    npx playwright test --project=webkit
    ```

7. **Run tests in headed mode:**
    ```bash
    npx playwright test --headed
    ```

8. **Run a specific test file:**
    ```bash
    npx playwright test tests/example.spec.ts
    ```

9. **Run tests with a specific configuration:**
    ```bash
    npx playwright test --config=playwright.config.ts
    ```

For more information, refer to the [Playwright documentation](https://playwright.dev/docs/intro).
