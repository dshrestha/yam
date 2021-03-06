package yam

import yam.adapters.DbmsAdapter
import yam.annotations.ChangeSet
import yam.changeLogs.DbChangeLog
import yam.migrationScript.MigrationScript

import java.lang.reflect.Method

class DbChangeSetService {

    def connectionManagerService
    def loggerService

    /**
     * Returns all the changeSet methods within a changeLog class
     *
     * @param changeLog class
     * @return list of methods
     * */
    List<Method> getChangeSets(Class changeLog) {
        List<Method> changeSets = changeLog.declaredMethods.findAll() {
            return it.isAnnotationPresent(ChangeSet.class)
        }

        return changeSets.sort {
            ChangeSet changeSet = it.getAnnotation(ChangeSet.class)
            return changeSet.sequence()
        }
    }

    /**
     * Gets resource name based on change set and change lof
     *
     * @param changeSet method
     * @param changeLog instance
     * @param capitalize
     * @return string
     * */
    String getResourceName(Method changeSet, DbChangeLog changeLogInstance, Boolean capitalize = true) {
        String resourceName = changeLogInstance.class.name + "." + changeSet.name
        if (capitalize) {
            resourceName.toUpperCase()
        }
        return resourceName
    }

    /**
     * Checks if the changeSet record already exists or not
     *
     * @param changeSet method
     * @param changeLog instance
     * @param action update|rollback
     * @return boolean
     * */
    boolean changeSetExists(Method changeSet, Object changeLogInstance, String action = "update") {
        ChangeSet annotation = changeSet.getAnnotation(ChangeSet.class)
        DbmsAdapter adapter = getAdapter(annotation)
        return adapter.changeSetExists(getResourceName(changeSet, changeLogInstance), annotation.version(), action)
    }

    /**
     * Inserts a changeSet record in the target db
     *
     * @param changeSet method
     * @param changeLog instance
     * @param action update|rollback
     * @return void
     * */
    def insert(Method changeSet, Object changeLogInstance, String action = "update") {
        ChangeSet annotation = changeSet.getAnnotation(ChangeSet.class)
        DbmsAdapter adapter = getAdapter(annotation)
        adapter.insertChangeSet([
                'resource'       : getResourceName(changeSet, changeLogInstance),
                'version'        : annotation.version(),
                'changeLogClass' : changeLogInstance.class.name,
                'changeSetMethod': changeSet.name,
                'runGroup'       : adapter.nextRunGroup,
                'author'         : annotation.author(),
                'date'           : new Date(),
                'action'         : action
        ])
    }

    DbmsAdapter getAdapter(ChangeSet annotation) {
        return connectionManagerService.getAdapter(annotation.changeSetConnection())
    }

    /**
     * Runs update script
     *
     * @return Void
     * */
    def update(Method changeSet, DbChangeLog changeLogInstance) {
        String action = "update"
        String resourceName = getResourceName(changeSet, changeLogInstance, false)
        ChangeSet annotation = changeSet.getAnnotation(ChangeSet.class)
        DbmsAdapter adapter = getAdapter(annotation)

        if (annotation.runAlways() || !changeSetExists(changeSet, changeLogInstance, action)) {
            try {
                Long start = new Date().getTime()
                MigrationScript ret = changeSet.invoke(changeLogInstance, adapter.db())
                ret.update()
                Long end = new Date().getTime()
                loggerService.logMessage("Ran ${resourceName} successfully. [${end - start} milli seconds]")
                insert(changeSet, changeLogInstance, action)
            } catch (Exception e) {
                loggerService.logMessage("Found exception when running ${resourceName}. ${e.message}")
                throw e
            }
        } else {
            loggerService.logMessage("Skipping ${resourceName} since it has already been run.")
        }
    }

    /**
     * Runs update script
     *
     * @return Void
     * */
    def rollback(Method changeSet, DbChangeLog changeLogInstance, Long toRunGroup) {
        String action = "rollback"
        String resourceName = getResourceName(changeSet, changeLogInstance, false)
        ChangeSet annotation = changeSet.getAnnotation(ChangeSet.class)

        DbmsAdapter adapter = getAdapter(annotation)
        Long runGroup = adapter.nextRunGroup
        while ((--runGroup) >= toRunGroup) {
            List changeSetData = adapter.list(['resourceName': resourceName, 'version': annotation.version(), 'action': 'update', 'runGroup': runGroup])
            if (changeSetData && changeSetData.size() && !changeSetExists(changeSet, changeLogInstance, 'rollback')) {
                try {
                    Long start = new Date().getTime()
                    MigrationScript ret = changeSet.invoke(changeLogInstance, adapter.db())
                    ret.rollback()
                    Long end = new Date().getTime()
                    loggerService.logMessage("Rolled back ${resourceName} successfully. [${end - start} milli seconds]")
                    insert(changeSet, changeLogInstance, action)
                } catch (Exception e) {
                    loggerService.logMessage("Found exception when running ${resourceName}. ${e.message}")
                    throw e
                }
            }
        }
    }
}
