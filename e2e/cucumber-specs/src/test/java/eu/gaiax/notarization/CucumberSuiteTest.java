package eu.gaiax.notarization;

import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
//@CucumberOptions(dryRun = true, monochrome = true, plugin = "unused:target/unused.log")
public class CucumberSuiteTest extends CucumberQuarkusTest {

    public static void main(String[] args) {
        runMain(CucumberSuiteTest.class, args);
    }
}
