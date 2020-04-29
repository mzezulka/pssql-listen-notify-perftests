package cz.fi.muni.pa036.listennotify.pssql.listen.notify.perftests;

import cz.fi.muni.pa036.listennotify.api.AbstractListenNotifyClient;
import cz.fi.muni.pa036.listennotify.api.event.EventType;
import cz.fi.muni.pa036.listennotify.api.CrudClient;
import java.util.concurrent.Executors;

public class ClientProcess {
    public static void main(String... args) {
        AbstractListenNotifyClient lncb = ListenNotifyClientFactory.client();
        CrudClient ccj = CrudClientFactory.client();
        ccj.registerEventListener(EventType.INSERT_TEXT);
        lncb.setCrudClient(ccj);
        Executors.newSingleThreadExecutor().execute(lncb);
        while(true) lncb.nextText();
    }  
}
