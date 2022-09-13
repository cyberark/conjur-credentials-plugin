package org.conjur.jenkins.conjursecrets;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;

import org.conjur.jenkins.credentials.ConjurCredentialStore;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.credentialsbinding.BindingDescriptor;
import org.jenkinsci.plugins.credentialsbinding.MultiBinding;
import org.jenkinsci.plugins.credentialsbinding.impl.CredentialNotFoundException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.model.TaskListener;

public class ConjurSecretCredentialsBinding extends MultiBinding<ConjurSecretCredentials> {

	@Symbol("conjurSecretCredential")
	@Extension
	public static class DescriptorImpl extends BindingDescriptor<ConjurSecretCredentials> {

		@Override
		public String getDisplayName() {
			return "Conjur Secret credentials";
		}

		@Override
		public boolean requiresWorkspace() {
			return false;
		}

		@Override
		protected Class<ConjurSecretCredentials> type() {
			return ConjurSecretCredentials.class;
		}
	}

	private static final Logger LOGGER = Logger.getLogger(ConjurSecretCredentialsBinding.class.getName());

	private String variable;

	@DataBoundConstructor
	public ConjurSecretCredentialsBinding(String credentialsId) {
		super(credentialsId);
	}

	@Override
	public MultiEnvironment bind(Run<?, ?> build, FilePath workSpace, Launcher launcher, TaskListener listener)
			throws IOException, InterruptedException {
		
		ModelObject objectToFindStore = build;
		ConjurSecretCredentials conjurSecretCredential = null;

		do {
			try {
				LOGGER.log(Level.FINE, "**** binding **** : " + objectToFindStore);
				ConjurCredentialStore store = null;
				if (objectToFindStore instanceof Run) {
					Run<?, ?> run = (Run<?, ?>) objectToFindStore;
					store = ConjurCredentialStore.getAllStores().get(String.valueOf(run.getParent().hashCode()));
				}
				if (store != null) {
					store.getProvider().getStore(objectToFindStore);
				}
				conjurSecretCredential = getCredentials(build);
				LOGGER.log(Level.FINE, "**** FOUND ID " + this.getCredentialsId() + " At Level : " + objectToFindStore);
			} catch (CredentialNotFoundException e) {
				if (objectToFindStore instanceof Run) {
					Run<?, ?> run = (Run<?, ?>) objectToFindStore;
					objectToFindStore = run.getParent().getParent();
				} else{
					Item g = (Item) objectToFindStore;
					
					objectToFindStore = g.getParent();
					if (!(objectToFindStore instanceof AbstractFolder)) {
						throw e;
					}
				}
			}	
		} while ((objectToFindStore instanceof AbstractFolder) && conjurSecretCredential == null);
		
		conjurSecretCredential.setContext(objectToFindStore);

		return new MultiEnvironment(
				Collections.singletonMap(variable, conjurSecretCredential.getSecret().getPlainText()));
	}

	public String getVariable() {
		return this.variable;
	}

	@DataBoundSetter
	public void setVariable(String variable) {
		LOGGER.log(Level.FINE, "Setting variable to {0}", variable);
		this.variable = variable;
	}

	@Override
	protected Class<ConjurSecretCredentials> type() {
		return ConjurSecretCredentials.class;
	}

	@Override
	public Set<String> variables() {
		return Collections.singleton(variable);
	}

}
