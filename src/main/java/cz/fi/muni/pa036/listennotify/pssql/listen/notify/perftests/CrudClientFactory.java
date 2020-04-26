package cz.fi.muni.pa036.listennotify.pssql.listen.notify.perftests;

import cz.fi.muni.pa036.listennotify.api.CrudClient;
import cz.fi.muni.pa036.listennotify.client.blocking.CrudClientJdbc;

class CrudClientFactory {
    private static final String CLIENT_PROP_NAME = "cz.fi.muni.pa036.client";
    private static final String CLIENT_PROP = System.getProperty(CLIENT_PROP_NAME);
    
    static {
        if(CLIENT_PROP == null) {
            throw new IllegalArgumentException(String.format("You have not specified system property '%s'. Please fix.", CLIENT_PROP_NAME));
        }
    } 
    
    public static CrudClient client() {
        switch(CLIENT_PROP) {
            case "nonblocking" : 
            case "blocking" : return new CrudClientJdbc();
            default: throw new IllegalArgumentException(String.format("Invalid value '%s' for property '%s'", 
                    CLIENT_PROP, CLIENT_PROP_NAME));
        }
    }
}
