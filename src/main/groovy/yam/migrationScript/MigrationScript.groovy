package yam.migrationScript

import yam.ConnectionManagerService

class MigrationScript {

    ConnectionManagerService connectionManagerService

    def db

    Closure update

    Closure rollback
}
