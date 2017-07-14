package yam.adapters

import com.mongodb.BasicDBObject
import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.MongoDatabase
import org.bson.types.ObjectId
import java.util.regex.Pattern

class MongoAdapter implements DbmsAdapter {

    def getObjectId = { id ->
        return (id instanceof ObjectId ? id : new ObjectId(id))
    }

    static Pattern uriMatcher() {
        return ~'^mongodb://(.*)'
    }

    MongoDatabase getDefaultDatabase() {
        MongoClient mongoClient = connectionResource
        MongoClientURI mongoClientURI = new MongoClientURI(this.connectionURI)
        String databaseName = mongoClientURI.database
        MongoDatabase db = mongoClient.getDatabase(databaseName)
        return db
    }

    def setup() {
        MongoDatabase db = getDefaultDatabase()
        List collectionNames = db.listCollectionNames().toList()
        if (!collectionNames.contains("changeSet")) {
            db.createCollection("changeSet", new BasicDBObject())
            db.getCollection('changeSet').createIndex(["resource": 1, "version": 1, "action": 1])
        }
    }

    def createConnection() {
        MongoClientURI mongoClientURI = new MongoClientURI(this.connectionURI)
        MongoClient mongoClient = new MongoClient(mongoClientURI)
        return mongoClient
    }

    Long getMaxRunGroup() {
        Long maxRunGroup = 0
        MongoDatabase db = getDefaultDatabase()
        db.getCollection("changeSet").find().sort(["runGroup": +1]).limit(1).each({
            maxRunGroup = it.runGroup ?: 0
        })
        return maxRunGroup
    }

    def db() {
        return getDefaultDatabase()
    }

    def closeConnection() {
        connectionResource?.close()
    }

    boolean changeSetExists(String resource, String version, String action) {
        List changeSets = list(['resource': resource, version: version, action: action])
        return changeSets.size() > 0
    }

    List list(Map filterCriteria) {
        MongoDatabase db = getDefaultDatabase()
        def queryFilter = [:]
        def filterKeys = [
                "id"      : ['key': '_id', 'process': { value -> getObjectId(value) }],
                "_id"     : ['key': '_id', 'process': { value -> getObjectId(value) }],
                "resource": ['key': 'resource', 'process': { value -> value }],
                "version" : ['key': 'version', 'process': { value -> value }],
                "action"  : ['key': 'action', 'process': { value -> value }]
        ]
        filterCriteria.collect { key, value ->
            if (filterKeys.keySet().contains(key)) {
                queryFilter[filterKeys[key]['key']] = filterKeys[key]['process'](value)
            }
        }
        return db.getCollection("changeSet").find(queryFilter).collect {
            return it
        }
    }

    Map insertChangeSet(Map changeSet) {
        MongoDatabase db = getDefaultDatabase()
        db.getCollection("changeSet").insertOne(changeSet)
        return changeSet
    }
}
