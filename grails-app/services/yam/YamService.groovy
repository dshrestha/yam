package yam

import yam.annotations.ChangeSet

import java.lang.reflect.Method


class YamService {

    def dbChangeLogService
    def connectionManagerService
    def dbChangeSetService

    Set<String> connectionsUsedInMigration() {
        Set<String> usedConnections = []
        dbChangeLogService.getChangeLogs().each { Class changeLog ->
            dbChangeSetService.getChangeSets(changeLog).each { Method changeSet ->
                ChangeSet annotation = changeSet.getAnnotation(ChangeSet.class)
                usedConnections.add(annotation.changeSetConnection())
            }
        }

        return usedConnections
    }

    def update() {
        try {
            connectionManagerService.initializeAdapters(connectionsUsedInMigration())
            dbChangeLogService.update()
        } catch (Exception e) {
            throw e
        } finally {
            connectionManagerService.closeConnections()
        }
    }
}
