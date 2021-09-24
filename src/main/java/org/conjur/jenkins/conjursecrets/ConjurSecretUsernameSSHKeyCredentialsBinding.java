package org.conjur.jenkins.conjursecrets;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.conjur.jenkins.credentials.ConjurCredentialStore;
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

		LOGGER.log(Level.FINE, "Binding UserName and SSHKey");

		ConjurCredentialStore store = ConjurCredentialStore.getAllStores().get(String.valueOf(build.getParent().hashCode()));
		if (store != null) {
			store.getProvider().getStore(build);
		}

		ConjurSecretUsernameSSHKeyCredentials conjurSecretCredential = getCredentials(build);
		conjurSecretCredential.setContext(build);

		Map<String, String> m = new HashMap<>();
		String usernameValue = conjurSecretCredential.getUsername();
		String secretValue = conjurSecretCredential.getPrivateKey();
		
		m.put(usernameVariable, usernameValue);
		m.put(secretVariable, secretValue);
		return new MultiEnvironment(m);

	}

	public String getSecretVariable() {
		return this.secretVariable;
	}

	public String getUsernameVariable() {
		return this.usernameVariable;
	}

	@DataBoundSetter
	public void setSecretVariable(String secretVariable) {
		LOGGER.log(Level.FINE, "Setting Secret variable to {0}", secretVariable);
		this.secretVariable = secretVariable;
	}

	@DataBoundSetter
	public void setUsernameVariable(String usernameVariable) {
		LOGGER.log(Level.FINE, "Setting Username variable to {0}", usernameVariable);
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
