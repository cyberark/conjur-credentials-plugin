package org.conjur.jenkins.conjursecrets;

import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.util.Secret;
import okhttp3.OkHttpClient;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ConjurSecretCredentialsImpl.class, Secret.class, OkHttpClient.class})
@PowerMockIgnore({ "javax.management.", "com.sun.org.apache.xerces.", "javax.xml.", "org.xml.", "org.w3c.dom.","com.sun.org.apache.xalan.", "javax.activation.*", "jdk.internal.reflect.*,java.util.logging.*" ,"javax.xml.*","org.xml.*", "javax.management.*, org.mockito.*"})
public class ConjurSecretCredentialImplTest {

	
	@Test
	public void mockGetSecret(){
    
     PowerMockito.mockStatic(ConjurSecretCredentialsImpl.class);
     ConjurSecretCredentialsImpl conjurSecretCredentials= mock(ConjurSecretCredentialsImpl.class);
     PowerMockito.mockStatic(Secret.class);
     Secret secret= mock(Secret.class); 
	 PowerMockito.when(conjurSecretCredentials.getSecret()).thenReturn(secret);

}
	
}