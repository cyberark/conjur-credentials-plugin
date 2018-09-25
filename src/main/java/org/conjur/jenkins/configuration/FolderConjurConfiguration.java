package org.conjur.jenkins.configuration;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;

import hudson.Extension;

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
}
