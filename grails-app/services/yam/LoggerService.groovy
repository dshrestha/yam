package yam

class LoggerService {

    /**
     * Method to log messages
     *
     * @param message
     * @return Void
     * */
    def logMessage(String message) {
        println(message)
    }

    def logMessage(Exception ex) {
        logMessage(ex.message)
    }
}
