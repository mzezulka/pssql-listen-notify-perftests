package cz.fi.muni.pa036.listennotify.pssql.listen.notify.perftests;

import cz.fi.muni.pa036.listennotify.api.AbstractListenNotifyClient;
import cz.fi.muni.pa036.listennotify.api.CrudClient;
import cz.fi.muni.pa036.listennotify.api.event.EventType;
import java.io.File;
import java.util.List;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.infra.Blackhole;
import static cz.fi.muni.pa036.listennotify.pssql.listen.notify.perftests.PropertyHelper.*;
import java.util.ArrayList;

/**
 *
 * Tests designed to be run only in a single thread.
 *
 * @author Miloslav Zezulka
 */
@State(Scope.Benchmark)
public class Perftests {

	private static final CrudClient crudClient = CrudClientFactory.client();
	private static final AbstractListenNotifyClient listenNotifyClient = ListenNotifyClientFactory.client();
	private final ExecutorService es = Executors.newSingleThreadExecutor();

	private int id = 1;
	private static final String DEFAULT_MESSAGE = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";
	private static final String INSERT_TEXT_MESSAGE = "INSERT INTO text VALUES (%d, '%s');";
	private static final String UPDATE_TEXT = "UPDATE text SET value ='changed';";

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

	// This benchmark does not use prepared statement, creates always a fresh one
    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    public boolean insertNaive() {
        try {
            crudClient.executeStatement(String.format(INSERT_TEXT_MESSAGE, id++, DEFAULT_MESSAGE));
            listenNotifyClient.nextText();
            return true;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    public boolean insertBasic() {
        try {
            crudClient.insertText(id++, DEFAULT_MESSAGE);
            listenNotifyClient.nextText();
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
            if (fileContents != null) {
                return;
            }
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream is = classLoader.getResourceAsStream("five_hundred.txt");
            Scanner s = new Scanner(is).useDelimiter("\\A");
            fileContents = s.hasNext() ? s.next().replace("\n", "") : "";
            assert (fileContents.length() > 300);
        }
    }

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    public boolean insertHundredsOfChars(HundredCharsState hcs) {
        try {
            crudClient.insertText(id++, hcs.fileContents);
            listenNotifyClient.nextText();
            return true;
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle);
        }
    }

    @State(Scope.Benchmark)
    public static class HundredsOfThousandsCharsState {

        String fileContents = null;

        @Setup
        public void setup() {
            if (fileContents != null) {
                return;
            }
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream is = classLoader.getResourceAsStream("three_hundred_thousand.txt");
            Scanner s = new Scanner(is).useDelimiter("\\A");
            fileContents = s.hasNext() ? s.next().replace("\n", "") : "";
            assert (fileContents.length() > 300_000);
        }
    }

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    public boolean insertHundredsOfThousandsOfChars(HundredsOfThousandsCharsState hcs) {
        try {
            crudClient.insertText(id++, hcs.fileContents);
            listenNotifyClient.nextText();
            return true;
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle);
        }
    }

    @State(Scope.Benchmark)
    public static class ImageState {

        FileInputStream fis = null;

        @Setup
        public void setup() {
            if (fis != null) {
                return;
            }
            try {
                fis = new FileInputStream(new File("src/main/resources/postgresql-logo.png"));
            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    public boolean insertTextAndImage(ImageState is) {
        try {
            // we need to insert text as well since the bin table
            // contains a foreign key (used in later scenarios)
            id++;
            crudClient.insertText(id, DEFAULT_MESSAGE);
            listenNotifyClient.nextText();
            crudClient.insertBinary(id, is.fis);
            listenNotifyClient.nextBinary();
            return true;
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle);
        }
    }

    // This scenario measures overhead caused by accepting data from two different channels
    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    public boolean insertTextAndImageAndDelete(ImageState is) {
        try {
            id++;
            crudClient.insertText(id, DEFAULT_MESSAGE);
            listenNotifyClient.nextText();
            crudClient.insertBinary(id, is.fis);
            listenNotifyClient.nextBinary();

            crudClient.deleteText(id);
            // note that we expect two messages to be reported, one for each table
            listenNotifyClient.nextText();
            listenNotifyClient.nextBinary();
            return true;
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle);
        }
    }

    @State(Scope.Benchmark)
    public static class NotifyScatterUpdateState {

        @Param({"10", "1000"})
        private int numInserts;

        @Setup(Level.Invocation)
        public void setup() {
            try {
                if (numInserts <= 10) {
                    for (int i = 0; i < numInserts; i++) {
                        crudClient.insertText(i, DEFAULT_MESSAGE);
                    }
                } else {
                    crudClient.insertBulkText();
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    // We first insert a certain amount of rows into the table (in the prepare phase
    // of the benchmark) and then execute a statement which affects all such rows
    @Benchmark
    @BenchmarkMode({Mode.SingleShotTime})
    public boolean oneSqlStatementMultipleNotify(Blackhole bh, NotifyScatterUpdateState nsus) {
        try {
            crudClient.executeStatement(UPDATE_TEXT);
            bh.consume(listenNotifyClient.nextText(nsus.numInserts));
            return true;
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle);
        }
    }

	@State(Scope.Benchmark)
	public static class MultiprocessState {

		@Param({ "2", "4", "8", "32" })
		private int noProcesses;

		private List<Process> listeners = new ArrayList<>();
		private FileInputStream fis;
		private File tmp = new File("/tmp/listen-notify-buffer.tmp");

		@Setup(Level.Trial)
		public void setup() {
			listeners.clear();
			// Spawn n new processes, each representing new client
			try {
				tmp.createNewFile();
				tmp.deleteOnExit();
				fis = new FileInputStream(tmp);
				for (int i = 0; i < noProcesses; i++) {

					listeners.add(Runtime.getRuntime().exec(String.format(
							"java -Dcz.fi.muni.pa036.client=%s -cp target/benchmarks.jar cz.fi.muni.pa036.listennotify.pssql.listen.notify.perftests.ClientProcess ",
							CLIENT_PROP)));

				}
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}

		@TearDown(Level.Trial)
		public void teardown() {
			listeners.forEach(p -> p.destroy());
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.AverageTime })
	public boolean multipleListeners(MultiprocessState mps) throws IOException {
		try {
			crudClient.insertText(id++, DEFAULT_MESSAGE);
			mps.listeners.forEach(p -> {
				try {
					mps.fis.read();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			});
			return true;
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}
}
