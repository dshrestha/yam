package yam

import grails.dev.commands.*
import org.springframework.beans.factory.annotation.Autowired

class UpdateCommand implements GrailsApplicationCommand {

    @Autowired
    YamService yamService

    boolean handle() {
        yamService.update()
        return true
    }
}
