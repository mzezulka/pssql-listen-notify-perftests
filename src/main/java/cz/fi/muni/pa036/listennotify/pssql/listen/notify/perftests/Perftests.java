package cz.fi.muni.pa036.listennotify.pssql.listen.notify.perftests;

import cz.fi.muni.pa036.listennotify.api.EventType;
import cz.fi.muni.pa036.listennotify.api.ListenNotifyClient;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Scanner;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.Blackhole;

/**
 *
 * @author Miloslav Zezulka
 */
@State(Scope.Thread)
public class Perftests {

    private ListenNotifyClient client = ListenNotifyClientFactory.client();
    private int id = 1;
    private static final String DEFAULT_MESSAGE = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";
    private static final String INSERT_TEXT_MESSAGE = "INSERT INTO text VALUES (%d, '%s');";
    private static final String INSERT_BINARY_MESSAGE = "INSERT INTO bin VALUES (%d, '%s');";

    @Setup(Level.Iteration)
    public void setup() throws SQLException {
        // clean all data
        client.executeStatement("DELETE FROM bin;");
        client.executeStatement("DELETE FROM text;");
        id = 1;
        for (EventType e : EventType.values()) {
            client.registerEventListener(e);
        }
    }

    @TearDown
    public void cleanup() {
        for (EventType e : EventType.values()) {
            client.deregisterEventListener(e);
        }
    }

    // This benchmark does not use prepared statement, always a fresh one
    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    public boolean insertNaive(Blackhole bh) {
        try {
            client.executeStatement(String.format(INSERT_TEXT_MESSAGE, id++, DEFAULT_MESSAGE));
            bh.consume(client.next());
            return true;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    public boolean insertBasic(Blackhole bh) {
        try {
            client.insertText(id++, DEFAULT_MESSAGE);
            bh.consume(client.next());
            return true;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    // Perftests scenarios dealing with different message lengths
    
    @State(Scope.Benchmark)
    public static class HundredCharsState {
        String fileContents = null;
        
        @Setup
        public void setup() {
            if(fileContents != null) return;
            InputStream is = getClass().getClassLoader().getResourceAsStream("/five_hundred.txt");
            Scanner s = new Scanner(is).useDelimiter("\\A");
            fileContents = s.hasNext() ? s.next() : "";
            assert(fileContents.length() == 500);
        }
    }
    
    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    public boolean insertHundredsOfChars(Blackhole bh, HundredCharsState hcs) {
        try {
            client.insertText(id++, hcs.fileContents);
            bh.consume(client.next());
            return true;
        } catch(SQLException sqle) {
            throw new RuntimeException(sqle);
        }
    }
    
    @State(Scope.Benchmark)
    public static class HundredsOfThousandsCharsState {
        String fileContents = null;
        
        @Setup
        public void setup() {
            if(fileContents != null) return;
            InputStream is = getClass().getClassLoader().getResourceAsStream("/three_hundred_thousand.txt");
            Scanner s = new Scanner(is).useDelimiter("\\A");
            fileContents = s.hasNext() ? s.next() : "";
            assert(fileContents.length() == 300_000);
        }
    }
    
    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    public boolean insertHundredsOfThousandsOfChars(Blackhole bh, HundredsOfThousandsCharsState hcs) {
        try {
            client.insertText(id++, hcs.fileContents);
            bh.consume(client.next());
            return true;
        } catch(SQLException sqle) {
            throw new RuntimeException(sqle);
        }
    }
    
    @State(Scope.Benchmark)
    public static class ImageState {
        FileInputStream fis = null;
        
        @Setup
        public void setup() {
            if(fis != null) return;
            try {
                fis = new FileInputStream(new File("src/main/resources/postgresql-logo.png"));
            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    
    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    public boolean insertTextAndImage(Blackhole bh, ImageState is) {
        try {
            // we need to insert text as well since the bin table
            // contains a foreign key (used in later scenarios)
            client.insertText(id++, DEFAULT_MESSAGE);
            bh.consume(client.next());
            client.insertBinary(id, is.fis);
            bh.consume(client.next());
            return true;
        } catch(SQLException sqle) {
            throw new RuntimeException(sqle);
        }
    }
    
    // Let's measure overhead caused by accepting data from two different channels
    // (FK constraint in bin has got CASCADE both on UPDATE and DELETE!)
    
    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    public boolean insertTextAndImageAndDelete(Blackhole bh, ImageState is) {
        try {
            client.insertText(id++, DEFAULT_MESSAGE);
            bh.consume(client.next());
            client.insertBinary(id, is.fis);
            bh.consume(client.next());
            client.deleteText(id);
            // note that we expect two messages to be reported, one for each table
            bh.consume(client.next());
            bh.consume(client.next());
            return true;
        } catch(SQLException sqle) {
            throw new RuntimeException(sqle);
        }
    }
}
 