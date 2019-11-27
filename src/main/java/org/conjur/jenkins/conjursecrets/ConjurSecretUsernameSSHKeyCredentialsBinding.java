package org.conjur.jenkins.conjursecrets;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.credentialsbinding.BindingDescriptor;
import org.jenkinsci.plugins.credentialsbinding.MultiBinding;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

public class ConjurSecretUsernameSSHKeyCredentialsBinding extends MultiBinding<ConjurSecretUsernameSSHKeyCredentials> {

	@Symbol("conjurSecretUsernameSSHKey")
	@Extension
	public static class DescriptorImpl extends BindingDescriptor<ConjurSecretUsernameSSHKeyCredentials> {

		@Override
		public String getDisplayName() {
			return "Conjur Secret Username SSHKey credentials";
		}

		@Override
		public boolean requiresWorkspace() {
			return false;
		}

		@Override
		protected Class<ConjurSecretUsernameSSHKeyCredentials> type() {
			return ConjurSecretUsernameSSHKeyCredentials.class;
		}
	}
	private static final Logger LOGGER = Logger.getLogger(ConjurSecretUsernameSSHKeyCredentialsBinding.class.getName());

	private String usernameVariable;

	private String secretVariable;

	@DataBoundConstructor
	public ConjurSecretUsernameSSHKeyCredentialsBinding(String credentialsId) {
		super(credentialsId);
	}

	@Override
	public MultiEnvironment bind(Run<?, ?> build, FilePath workSpace, Launcher launcher, TaskListener listener)
			throws IOException, InterruptedException {

		LOGGER.log(Level.INFO, "Binding UserName and SSHKey");

		ConjurSecretUsernameSSHKeyCredentials conjurSecretCredential = getCredentials(build);
		conjurSecretCredential.setContext(build);

		Map<String, String> m = new HashMap<>();
		m.put(usernameVariable, conjurSecretCredential.getUsername());
		m.put(secretVariable, conjurSecretCredential.getPrivateKey());
		return new MultiEnvironment(m);

	}

	public String getSecretVariable() {
		return this.secretVariable;
	}

	public String getUsernameVariable() {
		return this.usernameVariable;
	}

	@DataBoundSetter
	public void setPasswordVariable(String secretVariable) {
		LOGGER.log(Level.INFO, "Setting Password variable to {0}", secretVariable);
		this.secretVariable = secretVariable;
	}

	@DataBoundSetter
	public void setUsernameVariable(String usernameVariable) {
		LOGGER.log(Level.INFO, "Setting Username variable to {0}", usernameVariable);
		this.usernameVariable = usernameVariable;
	}

	@Override
	protected Class<ConjurSecretUsernameSSHKeyCredentials> type() {
		return ConjurSecretUsernameSSHKeyCredentials.class;
	}

	@Override
	public Set<String> variables() {
		return new HashSet<>(Arrays.asList(usernameVariable, secretVariable));
	}

}
