package yam

import grails.dev.commands.*
import org.springframework.beans.factory.annotation.Autowired

class UpdateCommand implements GrailsApplicationCommand {

    @Autowired
    YamService yamService

    boolean handle() {
        List srcPackages = System.getProperty("srcPackages") ? System.getProperty("srcPackages").split(",") : (System.getProperty("srcPackage") ? [System.getProperty("srcPackage")] : ["yam.changeLogs"])
        Boolean runParallel = String.valueOf(System.getProperty("runParallel")).equalsIgnoreCase("true")
        yamService.update(srcPackages, runParallel)
        return true
    }
}
