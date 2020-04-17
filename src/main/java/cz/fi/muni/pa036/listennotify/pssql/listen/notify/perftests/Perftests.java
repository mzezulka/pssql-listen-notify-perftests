package cz.fi.muni.pa036.listennotify.pssql.listen.notify.perftests;

import cz.fi.muni.pa036.listennotify.api.EventType;
import cz.fi.muni.pa036.listennotify.api.ListenNotifyClient;
import java.sql.SQLException;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

/**
 * 
 * @author Miloslav Zezulka
 */
@State(Scope.Thread)
public class Perftests {

    private ListenNotifyClient client = ListenNotifyClientFactory.client();
    
    @Setup
    public void setup() {
        for(EventType e : EventType.values()) {
            client.registerEventListener(e);
        }
    }
    
    @TearDown
    public void cleanup() {
        for(EventType e : EventType.values()) {
            client.deregisterEventListener(e);
        }
    }
    
    @Benchmark
    public boolean insertBasic(Blackhole bh) {
        bh.consume(insertBasicImple(bh));
        return true;
    }
    
    @Benchmark
    public boolean insertBasicImple(Blackhole bh)  {
        try {
            client.registerEventListener(EventType.INSERT);
            client.executeStatement("insert into dm_queue values (6,6,'here');");
            bh.consume(client.next());
            return true;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
