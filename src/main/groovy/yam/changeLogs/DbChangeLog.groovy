package yam.changeLogs

import yam.ConnectionManagerService
import yam.LoggerService

trait DbChangeLog {

    ConnectionManagerService connectionManagerService
    LoggerService loggerService

}
