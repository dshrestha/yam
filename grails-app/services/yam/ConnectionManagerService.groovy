package yam

import org.reflections.Reflections
import yam.adapters.DbmsAdapter

class ConnectionManagerService {

    /**
     * Property that holds adapters that were initialized for all the db connections
     */
    Map<String, DbmsAdapter> adapters = [:]

    /**
     * Initializes adapters. It looks up the system properties that matches with [connection-name]_uri
     * and for each matching uri, initializes respective adapter and also setups changeSet collection/table
     * if the db is used for migration.
     *
     * @param connectionsUsedInMigration Set of connection names that are used in migration
     * @return void
     * */
    def initializeAdapters(Set<String> connectionsUsedInMigration) {
        def arguments = System.properties
        Set<String> keys = arguments.keySet()
        keys.findAll({ it ==~ /(.*)(_uri)$/ }).each {
            String connectionName = it.substring(0, it.lastIndexOf("_uri"))
            DbmsAdapter adapter = getAdapter(connectionName)
            adapter.establishConnection()
            if (connectionsUsedInMigration.contains(connectionName)) {
                adapter.setup()
                adapter.nextRunGroup = adapter.getMaxRunGroup() + 1
            }
        }
    }

    /**
     * Given a connection name, this method returns the connection resource
     *
     * @param connectionName
     * @return connection resource
     * */
    def getConnection(String connectionName) {
        DbmsAdapter adapter = getAdapter(connectionName)
        if (adapter) {
            return adapter.db()
        }
    }

    /**
     * Closes all the connections that were created during migration
     *
     * @return Void
     * */
    def closeConnections() {
        adapters.each { String key, DbmsAdapter adapter ->
            adapter.closeConnection()
        }
    }

    /**
     * Returns a adapter class that can handle connection for given uri
     *
     * @param uri database connection uri
     * @return DbmsAdapter class
     * */
    DbmsAdapter getAdapterClass(String uri) {
        String srcPackage = "yam.adapters"
        Reflections reflections = new Reflections(srcPackage)
        Set<DbmsAdapter> adapters = (reflections.getSubTypesOf(DbmsAdapter.class))

        DbmsAdapter matchedAdapter = adapters.find({
            return uri.matches(it.uriMatcher())
        })
        return matchedAdapter
    }

    def getAdapter(String connectionName) {
        DbmsAdapter adapter = adapters.get(connectionName)
        if (adapter) {
            return adapter
        }

        String uri = System.getProperty(connectionName + "_uri")
        Class<?> clazz = getAdapterClass(uri)
        adapter = clazz.newInstance(connectionURI: uri)
        adapters.put(connectionName, adapter)
        return adapter
    }
}
