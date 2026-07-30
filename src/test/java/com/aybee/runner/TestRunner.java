package com.aybee.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = "src/test/resources/features",
        glue     = {"com.aybee.steps", "com.aybee.hooks"},
        plugin   = {
                "pretty",
                "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm",
                "json:target/cucumber-reports/cucumber.json",
                "html:target/cucumber-reports/cucumber.html"
        }
)
public class TestRunner extends AbstractTestNGCucumberTests {
}
