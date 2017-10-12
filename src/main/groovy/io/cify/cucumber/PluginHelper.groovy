package io.cify.cucumber

import io.cify.framework.core.CifyFrameworkException

/**
 * Created by FOB Solutions
 */
class PluginHelper {

    /**
     * Gets parameter from system
     * @param parameter
     * @return String
     */
    static String getParameter(String parameter) {
        if (System.getenv(parameter)) {
            return System.getenv(parameter)
        } else if (System.getProperty(parameter)) {
            return System.getProperty(parameter)
        } else {
            throw new CifyFrameworkException("User did not pass parameter for $parameter please add it to system environment variable or system property!")
        }
    }

}
