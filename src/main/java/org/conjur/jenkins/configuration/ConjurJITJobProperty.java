package org.conjur.jenkins.configuration;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import jenkins.model.Jenkins;

public class ConjurJITJobProperty<T extends Job<?, ?>> extends JobProperty<T> {
    
//    private static final Logger LOGGER = Logger.getLogger(ConjurJITJobProperty.class.getName());

	// Expose the following in the Resource Jelly to enable 
	// <f:entry title="Use Just-In-Time --JIT-- Secret Access" description="If checked, The Job identity will be used to authenticate to Conjur during execution of the job. You don't need to select a Conjur Auth Credential ">
	// 	<f:checkbox name="useJustInTime" field="useJustInTime" default="false" checked="${useJustInTime}"/>
	// </f:entry>
	// <f:entry title="${%Auth WebService ID}">
	// 	<f:textbox field="authWebServiceId" name="authWebServiceId"/>
	// </f:entry>
	// <f:entry title="${%Host Authentication Prefix}">
	// 	<f:textbox field="hostPrefix" name="hostPrefix"/>
	// </f:entry>

	private static final String DISPLAY_NAME = "Conjur Just-In-Time Access";

	private Boolean inheritFromParent = true;
    private Boolean useJustInTime = false;
    private String authWebServiceId = "";
    private String hostPrefix = "";
	private ConjurConfiguration conjurConfiguration;

	@DataBoundConstructor
	public ConjurJITJobProperty(ConjurConfiguration conjurConfiguration) {
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

    public Boolean getUseJustInTime() {
		return useJustInTime;
	}

	@DataBoundSetter
	public void setUseJustInTime(Boolean useJustInTime) {
		this.useJustInTime = useJustInTime;
    }

    public String getAuthWebServiceId() {
		return authWebServiceId;
	}

	@DataBoundSetter
	public void setAuthWebServiceId(String authWebServiceId) {
		this.authWebServiceId = authWebServiceId;
	}

    public String getHostPrefix() {

        return hostPrefix;
	}

	public Item getItem() {
		return Jenkins.get().getItemByFullName(this.owner.getFullName());
	}

	@DataBoundSetter
	public void setHostPrefix(String hostPrefix) {
		this.hostPrefix = hostPrefix;
	}

    @Extension
    public static final class ConjurJITJobPropertyDescriptorImpl extends JobPropertyDescriptor {

        /**
         * Getter function for a human readable class display name.
         *
         * @return a String containing the human readable display name for the {@link JobProperty} class.
         */
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }

        /**
         * Indicates where this property can be used
         *
         * @param jobType - a Job object
         * @return Always true. This property can be set for all Job types.
         */
        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return true;
        }
    }
}
