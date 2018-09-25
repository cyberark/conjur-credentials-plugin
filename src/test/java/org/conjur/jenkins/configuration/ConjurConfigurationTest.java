package org.conjur.jenkins.configuration;

import org.junit.Test;
import org.junit.Rule;
import org.jvnet.hudson.test.RestartableJenkinsRule;

public class ConjurConfigurationTest {

    @Rule
    public RestartableJenkinsRule rr = new RestartableJenkinsRule();

    /**
     * Tries to exercise enough code paths to catch common mistakes:
     * <ul>
     * <li>missing {@code load}
     * <li>missing {@code save}
     * <li>misnamed or absent getter/setter
     * <li>misnamed {@code textbox}
     * </ul>
     */
    @Test
    public void uiAndStorage() {
//        rr.then(r -> {
//            assertNull("not set initially", SampleConfiguration.get().getConjurConfiguration().getAccount());
//            HtmlForm config = r.createWebClient().goTo("configure").getFormByName("config");
//            HtmlTextInput textbox = config.getInputByName("_.account");
//            textbox.setText("hello");
//            r.submit(config);
//            assertEquals("global config page let us edit it", "hello", SampleConfiguration.get().getConjurConfiguration().getAccount());
//        });
//        rr.then(r -> {
//            assertEquals("still there after restart of Jenkins", "hello", SampleConfiguration.get().getConjurConfiguration().getAccount());
//        });
    }

}
