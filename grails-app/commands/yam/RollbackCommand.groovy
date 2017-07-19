package yam

import grails.dev.commands.GrailsApplicationCommand
import org.springframework.beans.factory.annotation.Autowired

class RollbackCommand implements GrailsApplicationCommand {

    @Autowired
    YamService yamService

    boolean handle() {
        List srcPackages = System.getProperty("srcPackages") ? System.getProperty("srcPackages").split(",") : (System.getProperty("srcPackage") ? [System.getProperty("srcPackage")] : ["yam.changeLogs"])
        Boolean runParallel = String.valueOf(System.getProperty("runParallel")).equalsIgnoreCase("true")
        Long toRunGroup = Long.valueOf(System.getProperty("runGroup"))
        yamService.rollback(srcPackages, toRunGroup)
        return true
    }
}
