# conjur-credentials-plugin
This Conjur plugin securely provides credentials to Jenkins jobs. Secrets are injected as environment variables to the build steps of a project. 

## Installation

### From Source

To build the plugin from source, you'll need Maven. You can build it like this:

```bash
git clone {repo}
cd conjur-credentials-plugin
mvn install -DskipTests
```
### From Binaries

As another option, you can use the latest .hpi found under the binaries folder.

### Install in Jenkins

When you have the .hpi file, log into Jenkins as an administrator. Then go to "Jenkins" -> "Manage Jenkins" -> "Manage Plugins" -> 	"Advanced". 
In the "Upload Plugin" section, browse for the .hpi and upload it to Jenkins:

![Upload Plugin](docs/images/UploadPlugin-Jenkins.png)

Make sure to restart Jenkins after the installation of the plugin:

![Install Plugin](docs/images/Plugin-Installing.png)



## Usage

### Conjur Login Credential

When the plugin is installed and Jenkins is restarted, start by storing the credential for Jenkins to connect to Conjur. 
The credential needs to be defined as standard "Username with password" credential. 

In the example below, the username is defined as host/frontend/frontend-01 which is defined in my conjur policy, and I use the API key for that host as the password. 

![Conjur Login Credential](docs/images/ConjurLogin-Credential.png)

### Global Configuration

You can define a global configuration for Conjur. This allows any job to use the configuration (unless it is overriden at folder level).

In Global Configuration, define the Conjur Account and Appliance URL to use. 

![Global Configuration](docs/images/GlobalConfiguration.png)

  

Requests to Conjur will fail unless: 
  ** An SSL Certificate is associated. 
     **Note**: The SSL Certificate can be linked to a certificate already stored in Jenkins (defined as credentials).
  ** A certificate is locally defined in the cacerts of the JVM where the requests is being sent from 
  ** Conjur is not set up to use SSL. 

### Folder Property Configuration

You can override the global configuration by setting the Conjur Appliance information at Folder level, if the checkbox "Inherit from parent?" is checked, it means it will ignore the values set here, and go up the level navigating to the parent folder, or taking the global configuration if all folder up the hierarchy are inheriting from parent. 

![Folder Property Configuration](docs/images/FolderConfiguration.png)

**Note**: The SSL Certificate can be linked to a certificate already stored in Jenkins (defined as credentials). If there is not SSL certificate associated, the requests to Conjur will fail unless there is a certificate locally defined in the cacerts of the JVM where the requests is being sent from or Conjur is not setup to use SSL. 

### Conjur Secret Definition

You must define the secrets that you want to access from Conjur explicitly. To do so,  define the secrets as credentials of kind "Conjur Secret Credential". 

![Conjur Secret Definition](docs/images/ConjurSecret-Credential.png)



###  Usage from a Jenkins pipeline script

You can use `withCredentials` and use the symbol `conjurSecretCredential` to make reference to the Conjur secrets.
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

Use the option "Use secret text(s) or file(s)" in the "Build Environment" section of a Freestyle project to bind to Conjur Secrets.

![Secret bindings on Freestyle Project](docs/images/SecretBindingsOnFreestyle.png)

Secrets are injected as environment variables to the build steps of the project. 


