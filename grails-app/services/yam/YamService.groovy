package yam

import grails.async.Promise

import static grails.async.Promises.*
import yam.annotations.ChangeSet
import java.lang.reflect.Method

class YamService {

    def dbChangeLogService
    def connectionManagerService
    def dbChangeSetService
    def loggerService

    /**
     * Returns list of connection that will be used for migration.
     * Sometimes we might define connection uri but we do not run migration script on them
     * but use those connection resource as a source for other migration. For eg. setting up
     * values based on values present in external db
     *
     * @param packages
     * @return set of connection names
     * */
    Set<String> connectionsUsedInMigration(List<String> packages) {
        Set<String> usedConnections = []
        packages.each {
            dbChangeLogService.getChangeLogs(it).each { Class changeLog ->
                dbChangeSetService.getChangeSets(changeLog).each { Method changeSet ->
                    ChangeSet annotation = changeSet.getAnnotation(ChangeSet.class)
                    usedConnections.add(annotation.changeSetConnection())
                }
            }
        }
        return usedConnections
    }

    /**
     * Runs the migrations in synchronous mode
     *
     * @param packages
     * @return void
     * */
    def update(List<String> packages) {
        packages.each { String packageName ->
            dbChangeLogService.update(packageName)
        }
    }

    /**
     * Runs the migrations in parallel mode
     *
     * @param packages
     * @return void
     * */
    def updateP(List<String> packages) {
        List<Promise> promises = []
        packages.each { String _packageName ->
            promises << task {
                String packageName = _packageName
                dbChangeLogService.update(packageName)
            }
        }
        waitAll(promises)
    }

    def update(List<String> packages, Boolean runParallel) {
        Long startTime = new Date().getTime()
        connectionManagerService.initializeAdapters(connectionsUsedInMigration(packages))
        try {
            runParallel ? updateP(packages) : update(packages)
        } catch (Exception e) {
            throw e
        } finally {
            Long endTime = new Date().getTime()
            loggerService.logMessage("Migration completed in ${endTime - startTime} milli seconds")
            connectionManagerService.closeConnections()
        }
    }

    def rollback(List<String> packages, Long toRunGroup) {
        Long startTime = new Date().getTime()
        connectionManagerService.initializeAdapters(connectionsUsedInMigration(packages))
        try {
            packages.each { String packageName ->
                dbChangeLogService.rollback(packageName, toRunGroup)
            }
        } catch (Exception e) {
            throw e
        } finally {
            Long endTime = new Date().getTime()
            loggerService.logMessage("Rollback completed in ${endTime - startTime} milli seconds")
            connectionManagerService.closeConnections()
        }
    }
}
