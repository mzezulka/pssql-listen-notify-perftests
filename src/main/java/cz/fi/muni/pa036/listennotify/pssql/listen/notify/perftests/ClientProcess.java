package cz.fi.muni.pa036.listennotify.pssql.listen.notify.perftests;

import cz.fi.muni.pa036.listennotify.api.AbstractListenNotifyClient;
import cz.fi.muni.pa036.listennotify.api.event.EventType;
import cz.fi.muni.pa036.listennotify.api.CrudClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;

public class ClientProcess {
    public static void main(String... args) {
		File tmp = new File("/tmp/listen-notify-buffer.tmp");
    	try(FileOutputStream fos = new FileOutputStream(tmp)) {
    		tmp.createNewFile();
    		AbstractListenNotifyClient lncb = ListenNotifyClientFactory.client();
            CrudClient ccj = CrudClientFactory.client();
            ccj.registerEventListener(EventType.INSERT_TEXT);
            lncb.setCrudClient(ccj);
            Executors.newSingleThreadExecutor().execute(lncb);
            while(!Thread.interrupted()) {
                lncb.nextText();
                fos.write(1);
            }	
    	} catch(IOException ioe) {
    		throw new RuntimeException(ioe);
    	}
    }  
}
