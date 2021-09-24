package org.conjur.jenkins.configuration;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.model.Item;
import jenkins.model.Jenkins;


public class FolderConjurConfiguration extends AbstractFolderProperty<AbstractFolder<?>> {

	private Boolean inheritFromParent = true;
	private ConjurConfiguration conjurConfiguration;

	@DataBoundConstructor
	public FolderConjurConfiguration(ConjurConfiguration conjurConfiguration) {
		super();
		this.conjurConfiguration = conjurConfiguration;
	}

	public ConjurConfiguration getConjurConfiguration() {
		return conjurConfiguration;
	}

	@DataBoundSetter
	public void setConjurConfiguration(ConjurConfiguration conjurConfiguration) {
		this.conjurConfiguration = conjurConfiguration;
	}

	public Boolean getInheritFromParent() {
		return inheritFromParent;
	}

	@DataBoundSetter
	public void setInheritFromParent(Boolean inheritFromParent) {
		this.inheritFromParent = inheritFromParent;
	}

	@Extension
	public static class DescriptorImpl extends AbstractFolderPropertyDescriptor {
	}

	public Item getItem() {
		return Jenkins.get().getItemByFullName(this.owner.getFullName());
	}

}
