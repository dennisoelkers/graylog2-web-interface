/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package selenium.tests.sessions;

import com.google.common.collect.Maps;
import org.fluentlenium.adapter.FluentTest;
import org.fluentlenium.adapter.util.SharedDriver;
import org.fluentlenium.core.annotation.Page;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.test.FakeApplication;
import selenium.pages.DashboardPage;
import selenium.pages.LoginPage;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import static org.fest.assertions.fluentlenium.FluentLeniumAssertions.assertThat;
import static play.test.Helpers.*;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@SharedDriver(type = SharedDriver.SharedType.PER_CLASS)
public class SessionsTest extends FluentTest {
    public static final int WEB_PORT = 9999;
    private static final Logger log = LoggerFactory.getLogger(SessionsTest.class);
    @Page
    public LoginPage loginPage;

    @Override
    public String getDefaultBaseUrl() {
        return "http://localhost:" + WEB_PORT + "/";
    }

    @Override
    public WebDriver getDefaultDriver() {
        String sauceUser = System.getenv("SAUCE_USERNAME");
        String saucePassword = System.getenv("SAUCE_ACCESS_KEY");

        // Decide whether to use sauceLabs or local browser to execute Selenium tests.
        RemoteWebDriver driver;
        if (sauceUser != null && saucePassword != null && !sauceUser.isEmpty() && !saucePassword.isEmpty()) {
            URL saucelabs = null;
            try {
                saucelabs = new URL("http://" + sauceUser + ":" + saucePassword + "@localhost:4445/wd/hub");
            } catch (MalformedURLException e) {
                // ignore
            }

            // https://saucelabs.com/docs/platforms
            DesiredCapabilities capabilities = DesiredCapabilities.firefox();
            capabilities.setCapability("platform", "Windows 8");
            capabilities.setCapability("version", "21");
            capabilities.setCapability("tunnel-identifier", System.getenv("TRAVIS_JOB_NUMBER"));

            driver = new RemoteWebDriver(saucelabs, capabilities);
        } else {
            driver = new FirefoxDriver();
        }
        return driver;
    }

    private FakeApplication getApp() {
        System.setProperty("skip.config.check", "true");
        Map<String, Object> options = Maps.newHashMap();
        options.put("application.secret", "qwertyqwertyqwertyqwerty");
        options.put("graylog2-server.uris", "http://localhost:12900");
        return fakeApplication(options);
    }

    @Test
    public void login() {
        running(testServer(WEB_PORT, getApp()), new Runnable() {
            @Override
            public void run() {
                loginPage.go();
                final DashboardPage dashboardPage = loginPage.loginAs("admin", "admin");
                assertThat(dashboardPage).isAt();
            }
        });
    }

    @Test
    public void loginErrorNoUser() {
        running(testServer(WEB_PORT, getApp()), new Runnable() {
            @Override
            public void run() {
                loginPage.go();
                final LoginPage loginPage1 = loginPage.loginWithError("admin", "");
                assertThat(loginPage1).isAt();
            }
        });
    }
}
