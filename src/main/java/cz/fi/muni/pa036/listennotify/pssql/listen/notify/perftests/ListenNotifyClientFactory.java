package cz.fi.muni.pa036.listennotify.pssql.listen.notify.perftests;

import cz.fi.muni.pa036.listennotify.api.AbstractListenNotifyClient;
import cz.fi.muni.pa036.listennotify.client.blocking.ListenNotifyClientBlocking;
import cz.fi.muni.pa036.listennotify.client.ng.ListenNotifyClientNg;

import static cz.fi.muni.pa036.listennotify.pssql.listen.notify.perftests.PropertyHelper.*;

/**
 * @author Miloslav Zezulka
 */
public class ListenNotifyClientFactory {
    
    static {
        if(CLIENT_PROP == null) {
            throw new IllegalArgumentException(String.format("You have not specified system property '%s'. Please fix.", CLIENT_PROP_NAME));
        }
    }
    
    public static AbstractListenNotifyClient client() {
        switch(CLIENT_PROP) {
            case "nonblocking" : return new ListenNotifyClientNg();
            case "blocking" : return new ListenNotifyClientBlocking();
            default: throw new IllegalArgumentException(String.format("Invalid value '%s' for property '%s'", 
                    CLIENT_PROP, CLIENT_PROP_NAME));
        }
    }
}
