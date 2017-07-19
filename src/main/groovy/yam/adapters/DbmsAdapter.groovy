package yam.adapters

import java.util.regex.Pattern

trait DbmsAdapter {

    /**
     * This property is used to group together the change sets that were run together
     *
     * @property nextRunGroup
     * */
    Long nextRunGroup

    /**
     * This property holds the connection resource
     *
     * @property connectionResource
     * */
    def connectionResource

    /**
     * This property holds the connection URI string used to establish connection
     *
     * @property connectionURI
     * */
    String connectionURI

    /**
     * Regex pattern for the connection URI the adapter can handle
     *
     * @return Pattern
     * */
    abstract static Pattern uriMatcher()

    /**
     * Method that is invoked every time a migration is run.
     * Setup should create changeSet table/collection as well as create indexes
     *
     * @return Void
     * */
    abstract def setup()

    /**
     * Method that establishes a database connection and returns the resource
     * */
    abstract def createConnection()

    /**
     * This method should return max runGroup value from the changeSet table
     * */
    abstract Long getMaxRunGroup()

    /**
     * This method closes the established connection
     * */
    abstract def closeConnection()

    /**
     * This method closes the established connection
     * */
    abstract List list(Map filterCriteria)

    /**
     * Method that returns true is change set exists for given resource or else returns false
     *
     * @param resource name
     * @param version
     * @param action update|rollback
     * @return boolean
     * */
    abstract boolean changeSetExists(String resource, String version, String action)

    /**
     * This method creates a change set and stores it in db
     *
     * @param changeSet
     * @return inserted changeSet record
     * */
    abstract Map insertChangeSet(Map changeSet)

    /**
     * Created a new connection and then stores the connection resource in the bean for re-use.
     * */
    def establishConnection() {
        if (!this.connectionResource) {
            this.connectionResource = createConnection()
        }
    }

    /**
     * Returns the database resource
     * */
    def db() {
        return this.connectionResource
    }

}