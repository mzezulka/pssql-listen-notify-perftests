package cz.fi.muni.pa036.listennotify.pssql.listen.notify.perftests;

import cz.fi.muni.pa036.listennotify.client.blocking.ListenNotifyClientNonblocking;
import cz.fi.muni.pa036.listennotify.api.ListenNotifyClient;

/**
 * @author Miloslav Zezulka
 */
public class ListenNotifyClientFactory {
    
    private static final String clientPropName = "cz.fi.muni.pa036.client";
    private static final String clientProp = System.getProperty(clientPropName);
    
    static {
        if(clientProp == null) {
            throw new IllegalArgumentException(String.format("You have not specified system property '%s'. Please fix.", clientPropName));
        }
    }
    
    public static ListenNotifyClient client() {
        switch(clientProp) {
            case "nonblocking" : 
            case "blocking" : return new ListenNotifyClientNonblocking();
            default: throw new IllegalArgumentException(String.format("Invalid value '%s' for property '%s'", 
                    clientProp, clientPropName));
        }
    }
}
