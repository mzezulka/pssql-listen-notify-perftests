package cz.fi.muni.pa036.listennotify.pssql.listen.notify.perftests;

import cz.fi.muni.pa036.listennotify.api.AbstractListenNotifyClient;
import cz.fi.muni.pa036.listennotify.api.CrudClient;
import cz.fi.muni.pa036.listennotify.api.event.EventType;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
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
public class MultithreadedPerftests {
    private final CrudClient crudClient = CrudClientFactory.client();
    private final AbstractListenNotifyClient listenNotifyClient = ListenNotifyClientFactory.client();
    private final ExecutorService es = Executors.newSingleThreadExecutor();
    
    private static final String DEFAULT_MESSAGE = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";

    private int id = 1;
    
    @Setup(Level.Trial)
    public void benchmarkSetup() {
        for (EventType e : EventType.values()) {
            crudClient.registerEventListener(e);
        }
        listenNotifyClient.setCrudClient(crudClient);
        es.execute(listenNotifyClient);
    }

    @TearDown(Level.Trial)
    public void perIterationSetup() {
        for (EventType e : EventType.values()) {
            crudClient.deregisterEventListener(e);
        }
        es.shutdownNow();
    }
    
    @Setup(Level.Iteration)
    public void setup() throws SQLException {
        // clean all data
        crudClient.executeStatement("DELETE FROM bin;");
        crudClient.executeStatement("DELETE FROM text;");
        id = 1;
    }
    
    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    public boolean insertBasic(Blackhole bh) {
        try {
            crudClient.insertText(id++, DEFAULT_MESSAGE);
            bh.consume(listenNotifyClient.nextText());
            return true;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
