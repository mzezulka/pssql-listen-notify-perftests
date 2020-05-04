package cz.fi.muni.pa036.listennotify.pssql.listen.notify.perftests;

import cz.fi.muni.pa036.listennotify.api.CrudClient;
import cz.fi.muni.pa036.listennotify.client.blocking.CrudClientJdbc;
import cz.fi.muni.pa036.listennotify.client.ng.CrudClientJdbcNg;

import static cz.fi.muni.pa036.listennotify.pssql.listen.notify.perftests.PropertyHelper.*;

class CrudClientFactory {

    static {
        if (CLIENT_PROP == null) {
            throw new IllegalArgumentException(String.format("You have not specified system property '%s'. Please fix.", CLIENT_PROP_NAME));
        }
    }

    public static CrudClient client() {
        switch (CLIENT_PROP) {
            case "nonblocking":
            	return new CrudClientJdbcNg();
            case "blocking":
                return new CrudClientJdbc();
            default:
                throw new IllegalArgumentException(String.format("Invalid value '%s' for property '%s'",
                        CLIENT_PROP, CLIENT_PROP_NAME));
        }
    }
}
