package org.conjur.jenkins.ConjurSecrets;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.jenkinsci.plugins.credentialsbinding.BindingDescriptor;
import org.jenkinsci.plugins.credentialsbinding.MultiBinding;
import org.kohsuke.stapler.DataBoundConstructor;

import org.apache.commons.lang.StringUtils;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

public class ConjurSecretCredentialsBinding extends MultiBinding<ConjurSecretCredentials> {

	private String secretVariable;
	private String descriptionVariable;

	@DataBoundConstructor
	public ConjurSecretCredentialsBinding(@Nullable String secretVariable, @Nullable String descriptionVariable, String credentialsId) {
		super(credentialsId);
		this.secretVariable = StringUtils.defaultIfBlank(secretVariable, "CONJUR_SECRET");
		this.descriptionVariable = StringUtils.defaultIfBlank(descriptionVariable, "CREDENTIAL_DESCRIPTION");
	}

	public String getSecretVariable() {
		return secretVariable;
	}

	@Override
	public MultiEnvironment bind(Run<?, ?> build, FilePath workSpace, Launcher launcher, TaskListener listener)
			throws IOException, InterruptedException {

		ConjurSecretCredentials conjurSecretCredential = this.getCredentials(build);
		conjurSecretCredential.setContext(build);

		Map<String, String> map = new HashMap<String, String>();
		map.put(this.secretVariable, conjurSecretCredential.getSecret().getPlainText());
		map.put(this.descriptionVariable,  conjurSecretCredential.getDescription());
		
		return new MultiEnvironment(map);
	}

	@Override
	protected Class<ConjurSecretCredentials> type() {
		return ConjurSecretCredentials.class;
	}

	@Override
	public Set<String> variables() {
        Set<String> variables = new HashSet<String>();
        variables.add(this.secretVariable);
        variables.add(this.descriptionVariable);
        return variables;	
    }
	
	@Extension
    public static class DescriptorImpl extends BindingDescriptor<ConjurSecretCredentials> {

        @Override
        protected Class<ConjurSecretCredentials> type() {
            return ConjurSecretCredentials.class;
        }

        @Override
        public String getDisplayName() {
            return "Conjur Secret credentials";
        }
    }
	
}
