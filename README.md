### Kind services


1. Configure kind2 program and arguments in ```src/main/resources/configurations.json```

For kind2 docker in Windows
```json
{
	"kindPath": "",
	"jobsDirectory": "jobs",
	"kindCheckCommand": "docker run -v \"{0}\":/lus kind2/kind2:dev /lus/{1} -xml",
	"kindMakeTableCommand": "docker run -v \"{0}\":/lus kind2/kind2:dev /lus/{1} --enable interpreter -xml",
	"kindInterpretCommand": "docker run -v \"{0}\":/lus kind2/kind2:dev /lus/{1} --enable interpreter --interpreter_input_file /lus/{2} -xml",
	"maxThreads": "24"
}
```

For milner server

```json
{
        "kindPath": "PATH=/usr/local/bin/",
	"jobsDirectory": "/var/lib/tomcat7/jobs",
	"kindCheckCommand": "/usr/local/bin/${kind2.binary} {0}/{1} -xml",
	"kindMakeTableCommand": "/usr/local/bin/${kind2.binary} {0}/{1} --enable interpreter -xml",
	"kindInterpretCommand": "/usr/local/bin/${kind2.binary} {0}/{1} --enable interpreter --interpreter_input_file {0}/{2} -xml",
	"maxThreads": "24"
}
```

2. Install  [Maven](https://maven.apache.org/download.cgi).
   For Ubuntu you can run ```sudo apt-get install maven```
3. Build the project using the command ```mvn package``` which will install 
the dependencies in the pom.xml, build the artifacts, and prepare the web archive file ```target/kindservices.war```.
4. Deploy ```kindservices.war``` file to the web container.  

## Deployment to milner server
 Milner server uses Apache Tomcat web container ```http://kind.cs.uiowa.edu:8080```
 
 To deploy the ```kindservices.war``` file you can use Tomcat Web Application Manager
 ```http://kind.cs.uiowa.edu:8080/manager/html``` to upload the file. 
 Please note the old deployment ```/kindservices``` needs to be undeployed first 
 before any new deployment.
 
 ## Deployment to a local tomcat server
  
1. Install [tomcat server](https://tomcat.apache.org/index.html). For Ubuntu you can 
run ```sudo apt-get install tomcat8 tomcat8-admin``` which will install the tomcat 
server and the admin web app.

2. The admin web app requires an admin user. This user can be added to 
```/var/lib/tomcat8/conf/tomcat-users.xml``` as follows: 
```xml
<tomcat-users >
...
<user username="admin" password="password" roles="manager-gui,admin-gui"/>
</tomcat-users>
``` 
The tomcat server needs to be restarted ```systemctl restart tomcat8```

3. The file ```kindservices.war``` could be uploaded using the manager page ```http://localhost:8080/manager```
Please note the old deployment ```/kindservices``` needs to be undeployed first 
 before any new deployment.
 