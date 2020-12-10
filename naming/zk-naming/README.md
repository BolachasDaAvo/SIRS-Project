# ZK-Naming

This is a Java library to simplify the access to Apache ZooKeeper, and to provide simple naming services.


## Instructions using Maven

To install package:
```
mvn install
```
(the JAR is made available in the local Maven repository ~/.m2/repository)

(other projects can now refer to the library as a dependency)

To generate API documentation:
```
mvn javadoc:javadoc
```


### To configure the Maven project in Eclipse

If the Maven pom.xml exists:

* 'File', 'Import...', 'Maven'-'Existing Maven Projects'
* 'Select root directory' and 'Browse' to the project base folder.
* Check that the desired POM is selected and 'Finish'.

If the Maven pom.xml does not exist:

* 'File', 'New...', 'Project...', 'Maven Projects'.
* Check 'Create a simple project (skip architype selection)'.
* Uncheck  'Use default Workspace location' and 'Browse' to the project base folder.
* Fill the fields in 'New Maven Project'.
* Check if everything is OK and 'Finish'.
