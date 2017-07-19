package yam.changeLogs.local2DB

import com.mongodb.client.MongoDatabase
import org.bson.Document
import yam.annotations.ChangeLog
import yam.annotations.ChangeSet
import yam.changeLogs.DbChangeLog
import yam.migrationScript.MigrationScript

@ChangeLog
class testChangeLog implements DbChangeLog {

    @ChangeSet(changeSetConnection = "local2DB", author = "deewendra.shrestha@gmail.com")
    MigrationScript createCollections(MongoDatabase db) {
        return new MigrationScript(
                update: {
                    Set collectionNames = db.listCollectionNames().toSet()
                    if (!collectionNames.contains("testCollection")) {
                        db.createCollection("testCollection")
                        (1..10000).each {
                            db.getCollection("testCollection").insertOne(new Document("name": it, "address":it))
                        }
                    }
                },
                rollback: {
                    Set collectionNames = db.listCollectionNames().toSet()
                    if (collectionNames.contains("testCollection")) {
                        db.getCollection("testCollection").drop()
                    }
                }
        )
    }

}
