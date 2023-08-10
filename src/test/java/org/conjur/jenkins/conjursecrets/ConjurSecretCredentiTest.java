package org.conjur.jenkins.conjursecrets;


import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.model.ModelObject;



@RunWith(PowerMockRunner.class)
@PrepareForTest({ConjurSecretCredentials.class, ModelObject.class })
@PowerMockIgnore({ "javax.management.", "com.sun.org.apache.xerces.", "javax.xml.", "org.xml.", "org.w3c.dom.","com.sun.org.apache.xalan.", "javax.activation.*", "jdk.internal.reflect.*,java.util.logging.*" ,"javax.xml.*","org.xml.*", "javax.management.*, org.mockito.*"})
public class ConjurSecretCredentiTest {

	@Test
	public void mockCredentialFromContextIfNeeded() throws IOException {
    
     PowerMockito.mockStatic(ConjurSecretCredentials.class);
     final ConjurSecretCredentials conjurSecretCredentials= mock(ConjurSecretCredentials.class);
     PowerMockito.mockStatic(ModelObject.class);
	 final ModelObject context= mock(ModelObject.class); 
	 String credentialID = "Id74";
	 PowerMockito.when(ConjurSecretCredentials.credentialFromContextIfNeeded(conjurSecretCredentials, credentialID, context)).thenReturn(conjurSecretCredentials);
	}
	
	
	@Test
	public void mockCredentialWithId() {
		
		 PowerMockito.mockStatic(ConjurSecretCredentials.class);
	     final ConjurSecretCredentials conjurSecretCredentialsId= mock(ConjurSecretCredentials.class);
		 PowerMockito.mockStatic(ModelObject.class);
		 final ModelObject context= mock(ModelObject.class);
		 String credentialID = "Id412";
		 PowerMockito.when(ConjurSecretCredentials.credentialWithID(credentialID, context)).thenReturn(conjurSecretCredentialsId);
		
	}
}
