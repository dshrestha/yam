package yam

import org.reflections.Reflections
import yam.annotations.ChangeLog
import yam.changeLogs.DbChangeLog
import java.lang.reflect.Method
import static grails.async.Promises.*

class DbChangeLogService {

    def dbChangeSetService
    def loggerService
    def connectionManagerService

    DbChangeLog instantiateChangeLog(Class<DbChangeLog> changeLog) {
        return changeLog.newInstance(
                connectionManagerService: connectionManagerService,
                loggerService: loggerService
        )
    }

    /**
     * Returns all the changeLog classes available for migration
     *
     * @return set of classes
     * */
    Set<Class<?>> getChangeLogs(String srcPackage = "yam.changeLogs") {
        Reflections reflections = new Reflections(srcPackage)
        Set<Class<?>> changeLogs = (reflections.getTypesAnnotatedWith(ChangeLog.class)).sort {
            ChangeLog changeLog = it.getAnnotation(ChangeLog.class)
            return changeLog.sequence()
        }
        return changeLogs
    }

    def update() {
        task {
            getChangeLogs().each { Class changeLog ->
                DbChangeLog changeLogInstance = instantiateChangeLog(changeLog)
                dbChangeSetService.getChangeSets(changeLog).each { Method changeSet ->
                    dbChangeSetService.update(changeSet, changeLogInstance)
                }
            }
        }
    }
}
