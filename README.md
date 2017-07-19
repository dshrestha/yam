# YAM
Grails application which can perform database migrations on any dbms platform using extensible adapters. This application uses very similar concept to other db migration tools such as mongobee(the annotations concepts were shamelessly taken from it) and Liquibase.

# Supported Commands
Unlike scripts, commands cause the Grails environment to start and you have full access to the application context and the runtime hence giving us more flexibility on customizing the application.

### 1) run

Runs the migration scripts. If a migration script has been run, then it won't run the same migration script again.
Example:
```bash
grails run-command run -Danalytics_uri="mongodb://testUser:password@localhost1:27017,localhost2:27017/databaseName?connectTimeoutMS=300000"
```

### 2) rollback

Reverts back a changeset that has already been executed.
Example:
```bash
grails run-command rollback -Dversion="1.0" -DchangeLogClass="migration.script.analytics.AnalyticsDatabaseChangeLog" -DchangeSetMethod="createCollectionIndexes" -Danalytics_uri="mongodb://affiservUser:Affinnova@localhost:27017/analytics?connectTimeoutMS=300000"
```

### command arguments/connection parameters
1) You can pass any arguments you would want/need but the adatpers will automatically establish connection if you provide arguments in following convention ***[changeSetConnectionName]_uri="[connection_uri]"***
2) If you want to run migration script from a specific change log then you can do so by providing ***srcPackage*** as an command argument which is just the package name under which the your changeLog class resides.

# Writing migration scripts
A migration script is basically a grovvy class which has to be placed within "yam.changeLogs" package or in its sub-package. The class represents a changeLog and the methods within them represents a changeSet to be run, however to qualify as a valid changeLog class it must be annotated with @ChangeLog and a method to qualify as a changeSet it must be annotated with @ChangeSet

Example:
```java
package yam.changeLogs

import com.mongodb.client.MongoDatabase
import yam.annotations.ChangeLog
import yam.annotations.ChangeSet
import yam.migrationScript.MigrationScript

@ChangeLog(sequence = 1)
class testChangeLog extends DbChangeLog {

    @ChangeSet(sequence = 1, version = "1.0", changeSetConnection = "analytics", dbms="mongo", author = "deewendra.shrestha@nielsen.com")
    MigrationScript createCollections(MongoDatabase db) {
        return new MigrationScript(
                update: {
                    db.createCollection("testCollection")
                },
                rollback: {
                    db.getCollection("testCollection").drop()
                }
        )
    }

}
```

* ***@ChangeLog*** annotaion accepts sequence parameter, by setting the sequence value you can guaratee the order in which the change logs will be executede
 
* ***@ChangeSet*** represents a actual migration script that will be run. It accepts following parameters:
    * ***sequence*** : sets the order in which changeSet will be executed(in ascending order)
    * ***version*** : version of code, once a changeSet has been run it won't re-run the changeSet unless the version is updated
    * ***changeSetConnection*** : connection to use
    * ***dbms*** : database system that will be used, if the dmbs is not supported you will have to create an adatper. More on this later
    * ***author*** : identity of user who created the script
    * ***comment*** : brief description on the nature of migration script 
    * ***runAlways*** : Executes the change set on every run, even if it has been run before

# Writing adapters for unsupported database system
If the dmbs that you are trying to use is not currently supported, then it is easy to add support for it. Create a class under ***yam.adapters*** package whose name should be of format ***[dbms]Adapter***. This class should implement yam.adapters.DbmsAdapter trait and implement the abstract methods. The abstract methods are:

* ***uriMatcher*** : pattern that matches the connection URI for the adapter 
* ***setup*** : create changeSet table and indexes on it
* ***createConnection*** : return a connection resource
* ***closeConnection*** :  close the connection
* ***changeSetExists*** :  given resource name, version and action this method should return true if the changeSet exists
* ***insertChangeSet*** : inserts a changeset records in the db

# Parallelization
You can also run the migration scritps in parallel but in order to take advantage of it, you need to organize your changelogs in separate packages. For eg, lets assume we have packages specific to the databse : `yam.changeLogs.myMongoDB, yam.changeLogs.mySqlDB, yam.changeLogs.myAnotherMongoDB`. In such a case you can run all the changelogs in those packages in parallel as follows

```bash
grails run-command run -DrunParallel=true -DsrcPackages="yam.changeLogs.myMongoDB, yam.changeLogs.mySqlDB, yam.changeLogs.myAnotherMongoDB" [connection uris]
 
```

Just make sure that the migration scripts are not inter-dependent when running them in parallel.
