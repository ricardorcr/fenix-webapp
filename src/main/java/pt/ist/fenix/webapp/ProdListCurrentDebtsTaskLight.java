package pt.ist.fenix.webapp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.events.AnnualEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEvent;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import org.joda.time.Period;

import com.google.gson.JsonObject;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */
@SuppressWarnings("Duplicates")
public class ProdListCurrentDebtsTaskLight extends CustomTask {

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    String getStudentNumber(Event event) {
        return event.getParty() instanceof Person ? event.getPerson().getUsername() : "-";
    }
    
    String getStudentName(Event event) {
        return event.getParty() instanceof Person ? event.getPerson().getName() : "Unit:" + event.getParty().getName();
    }

    String getSocialSecurityNumber(Event event) {
        return event.getParty() instanceof Person ? event.getPerson().getSocialSecurityNumber() : event.getParty().getSocialSecurityNumber();
    }

    String getGratuityExecutionYearName(Event event) {
        return event instanceof AnnualEvent ? ((AnnualEvent) event).getExecutionYear().getName() : event.getWhenOccured().toString("yyyy");
    }

    String getDegreeName(Event event) {
        return event instanceof GratuityEvent ? ((GratuityEvent) event).getDegree().getNameI18N().getContent() : "N/A";
    }

    String getDegreeTypeName(Event event) {
        return event instanceof GratuityEvent ? ((GratuityEvent) event).getDegree().getDegreeType().getName().getContent() : "N/A";
    }

    String getWhenOccured(Event event) {
        return event.getWhenOccured().toString("dd/MM/yyyy");
    }


    protected void warmup() {
        DateTime start = new DateTime();
        taskLog("started warmup");
        final Set<Object> o = new HashSet<>();
        o.addAll(Bennu.getInstance().getPartysSet());
        o.addAll(Bennu.getInstance().getAccountingEventsSet());
        o.addAll(Bennu.getInstance().getAccountingTransactionsSet());
        o.addAll(Bennu.getInstance().getEntriesSet());
        o.addAll(Bennu.getInstance().getAccountingTransactionDetailsSet());
        o.addAll(Bennu.getInstance().getExemptionsSet());
        o.addAll(Bennu.getInstance().getDiscountsSet());
        DateTime end = new DateTime();
        taskLog("finished warmup, took %s secs%n", new Period(start, end).toString());
    }

    final Path outputJson = new File("/tmp/all_closed_events.json").toPath();
    final Path outputLog = new File("/tmp/log.txt").toPath();

    @Override
    public void runTask() throws Exception {
        Files.write(outputJson, "[\n".getBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);

        warmup();

        final boolean[] running = new boolean[] { true };

        final Thread t = new Thread() {
            @Override
            public void run() {
                while (running[0]) {
                    try {
                        Thread.sleep(10000);
                    } catch (final InterruptedException e) {
                        throw new Error(e);
                    }
                    taskLog("Processed %s events%n", threadCount);
                }
            }
        };
        t.start();

        getEvents()
                .parallel()
                .map(e -> processEventTx(e))
                .filter(j -> j != null)
                .forEach(j -> {
                    try {
                        Files.write(outputJson, (j.toString() + ",\n").getBytes(), StandardOpenOption.APPEND);
                    } catch (final IOException e) {
                        throw new Error(e);
                    }
                });

        running[0] = false;

        Files.write(outputJson, "]".getBytes(), StandardOpenOption.APPEND);

        t.join();
    }

    private Stream<Event> getEvents() {
        return Bennu.getInstance().getAccountingEventsSet().stream();
    }

    private DateTime getLastCreditDate(final Event event) {
        final Stream<DateTime> exemptionDTs = event.getExemptionsStream().map(e -> e.getWhenCreated());
        final Stream<DateTime> discountDTs = event.getDiscountsSet().stream().map(e -> e.getWhenCreated());
        final Stream<DateTime> paymentRegisteredDTs = event.getNonAdjustingTransactionStream().map(e -> max(e.getWhenRegistered(), e.getWhenProcessed()));
        return Stream.concat(Stream.concat(exemptionDTs, discountDTs), paymentRegisteredDTs)
                .max(Comparator.naturalOrder()).orElseGet(() -> new DateTime());
    }

    private DateTime max(final DateTime dt1, final DateTime dt2) {
        return dt1.compareTo(dt2) > 0 ? dt1 : dt2;
    }

    private int threadCount = 0;
    private static final AtomicInstance ATOMIC_INSTANCE = new AtomicInstance(TxMode.READ, false);

    private JsonObject processEventTx(final Event event) {
        threadCount++;
        try {
            return FenixFramework.getTransactionManager().withTransaction(() -> processEvent(event), ATOMIC_INSTANCE);
        } catch (final Exception e) {
            throw new Error(e);
        }
    }

    private JsonObject processEvent(final Event event) {
        if (!event.isCancelled()) {
            try {
                final DateTime when = getLastCreditDate(event).plusSeconds(1);
                final Money amountToPay = event.calculateAmountToPay(when);

                if (!amountToPay.greaterThan(Money.ZERO)) {
                    final Money originalAmountToPay = event.getOriginalAmountToPay();

                    final JsonObject jsonEvent = new JsonObject();
                    jsonEvent.addProperty("id", event.getExternalId());
                    jsonEvent.addProperty("originalAmountToPay", originalAmountToPay.toPlainString());
                    jsonEvent.addProperty("lastCreditDate", when.toString());
                    return jsonEvent;
                }
            } catch (NullPointerException | UnsupportedOperationException | DomainException npe) {
                final StringBuilder log = new StringBuilder();
                log.append(event.getExternalId());
                log.append(" ");
                log.append(event.getClass().getSimpleName());
                log.append(" ");
                log.append(event.getDescription().toString());
                log.append(" ");
                log.append(org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace (npe));
                log.append("\n");
                try {
                    Files.write(outputLog, log.toString().getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                } catch (final IOException e) {
                    throw new Error(e);
                }
            }
        }
        return null;
    }

    private ExecutionYear executionYearOf(final Event event) {
        return event instanceof AnnualEvent ? ((AnnualEvent) event).getExecutionYear() : ExecutionYear.readByDateTime(event.getWhenOccured());
    }

    protected boolean isNotBefore(ExecutionYear year, Event e)  {
        return !executionYearOf(e).isBefore(year);
    }

}