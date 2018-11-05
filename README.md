# conjur-credentials-plugin
This Conjur plugin securely provides credentials to Jenkins jobs.  

## Installation

### From Source

To build the plugin from source, Maven is required. Build it like this:

```bash
git clone {repo}
cd conjur-credentials-plugin
mvn install -DskipTests
```
### From Binaries

As another option, you can use the latest .hpi found under the binaries folder.

### Install in Jenkins

When you have the .hpi file, log into Jenkins as an administrator. Then go to *Jenkins* -> *Manage Jenkins* -> *Manage Plugins* -> 	*Advanced*. 
In the "Upload Plugin" section, browse for the .hpi and upload it to Jenkins:

![Upload Plugin](docs/images/UploadPlugin-Jenkins.png)

After installing the plugin, restart Jenkins:

![Install Plugin](docs/images/Plugin-Installing.png)



## Usage

After installing the plugin and restarting Jenkins, you are ready to start.

### Conjur Login Credential

The first step is to store the credential required for Jenkins to connect to Conjur. 

 Define the credential as a standard "Username with password" credential. 

In the example below, the credentials are: 

* Username is host/frontend/frontend-01. This host is defined in Conjur policy. 
* Password is the API key for that host. 

![Conjur Login Credential](docs/images/ConjurLogin-Credential.png)

### Global Configuration

A global configuration allows any job to use the configuration (unless it is overridden at folder level).

On the **Global Configuration** page, define the Conjur Account and Appliance URL to use. 

![Global Configuration](docs/images/GlobalConfiguration.png)

 

### Folder Property Configuration

You can override the global configuration by setting the Conjur appliance information at folder level on the **Folder Property Configuration** page. 

If the checkbox "Inherit from parent?" is checked, the values set here are ignored, and values are taken from the parent folder.  If all folders up the hierarchy are set to inherit from its parent, the global configuration is used.

![Folder Property Configuration](docs/images/FolderConfiguration.png)

Requests to Conjur will fail unless: 

* An SSL certificate is specified in the SSL certificate field. 
  **Note**: The SSL Certificate can be linked to a certificate already stored in Jenkins (defined as credentials). 
* There is a certificate locally defined in the cacerts of the JVM sending the requests    
* Conjur is not set up to use SSL.   

 

### Conjur Secret Definition

The secrets that you want to access from Conjur must be defined explicitly. Use the **Conjur Secret Definition** page to define secrets as credentials of kind "Conjur Secret Credential". 

![Conjur Secret Definition](docs/images/ConjurSecret-Credential.png)



###  Usage from a Jenkins pipeline script

To reference Conjur secrets in a Jenkins script, use `withCredentials` and the symbol `conjurSecretCredential`.  
Here is an example showing how to fetch the secret from a Jenkins job pipeline definition.

```yml
node {
   stage('Work') {
      withCredentials([conjurSecretCredential(credentialsId: 'DB_PASSWORD', 
                                              variable: 'SECRET')]) {
         echo "Hello World $SECRET"
      }
   }
   }
   stage('Results') {
      echo "Finished!"
   }
}
```

### Usage from a Jenkins Freestyle Project

To bind to Conjur secrets, use the option "Use secret text(s) or file(s)" in the "Build Environment" section of a Freestyle project.

![Secret bindings on Freestyle Project](docs/images/SecretBindingsOnFreestyle.png)

Secrets are injected as environment variables to the build steps of the project. 


