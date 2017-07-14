package yam.changeLogs

import com.mongodb.client.MongoDatabase
import yam.annotations.ChangeLog
import yam.annotations.ChangeSet
import yam.migrationScript.MigrationScript

@ChangeLog
class testChangeLog implements DbChangeLog {

    @ChangeSet(changeSetConnection = "localDB", author = "deewendra.shrestha@gmail.com")
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
