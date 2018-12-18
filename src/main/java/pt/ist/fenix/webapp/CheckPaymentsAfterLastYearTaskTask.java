package pt.ist.fenix.webapp;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.academic.domain.accounting.Discount;
import org.fenixedu.academic.domain.accounting.Entry;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.Exemption;
import org.fenixedu.academic.domain.accounting.events.AnnualEvent;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import org.joda.time.Period;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.CallableWithoutException;
import pt.ist.fenixframework.FenixFramework;

public class CheckPaymentsAfterLastYearTaskTask extends CustomTask {
    private final DateTime when = new DateTime(2017, 12, 31, 23, 59, 59, 999);

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }


    @Override
    public void runTask() throws Exception {
        warmup();
        ExecutionYear ex = ExecutionYear.readExecutionYearByName("2012/2013");
        Multimap<ExecutionYear, Event> eventsByYear = HashMultimap.create();
        Map<ExecutionYear, Future<DateTime>> futures = new HashMap<>();

        Bennu.getInstance().getAccountingEventsSet().stream().filter(e -> !e.isCancelled())
                .filter(e -> executionYearOf(e).isAfter(ex) && e.getWhenOccured().isBefore(when))
                .filter(e -> !e.getNonAdjustingTransactions().isEmpty()).forEach(e -> {
                    eventsByYear.put(executionYearOf(e), e);
                });


        ExecutorService executorService = Executors.newFixedThreadPool(10);

        eventsByYear.keySet().forEach(e -> {
            Callable<DateTime> task = () -> FenixFramework.getTransactionManager().withTransaction(
                    (CallableWithoutException<DateTime>) () -> process(e, eventsByYear.get(e), when),
                    new AtomicInstance(Atomic.TxMode.READ, false));
            futures.put(e, executorService.submit(task));

        });

        executorService.shutdown();
        if (executorService.awaitTermination(90, TimeUnit.MINUTES)) {
//            output("log.txt", builder.toString().getBytes());
        }

        futures.keySet().stream().sorted(ExecutionYear.COMPARATOR_BY_YEAR).forEach(year -> {
            Future<DateTime> moneyFuture = futures.get(year);
            try {
                DateTime date = moneyFuture.get();
                taskLog("Year: %s Total: %s%n", year.getQualifiedName(), date.toString());
            } catch (InterruptedException | ExecutionException e) {
                throw new Error(e);
            }
        });

    }

    private DateTime process(ExecutionYear year, Collection<Event> events, DateTime when) {
        taskLog("Start for %s%n", year.getQualifiedName());
        return events.stream().flatMap(e -> e.getNonAdjustingTransactions().stream())
                .filter(t -> t.getWhenProcessed().isAfter(when) && t.getWhenRegistered().isBefore(when))
//             .sorted(Comparator.comparing(Event::getExternalId))
                .max(Comparator.comparing(AccountingTransaction::getWhenProcessed)).map(AccountingTransaction::getWhenProcessed)
                .orElse(new DateTime());

    }

    private Set<Party> parties = new HashSet<>();
    private Set<Event> events = new HashSet<>();
    private Set<AccountingTransaction> transactions = new HashSet<>();
    private Set<Entry> entries = new HashSet<>();
    private Set<AccountingTransactionDetail> details = new HashSet<>();
    private Set<Exemption> exemptions = new HashSet<>();
    private Set<Discount> discounts = new HashSet<>();

    protected void warmup() {
        DateTime start = new DateTime();
        taskLog("started warmup");
        parties.addAll(Bennu.getInstance().getPartysSet());
        events.addAll(Bennu.getInstance().getAccountingEventsSet());
        transactions.addAll(Bennu.getInstance().getAccountingTransactionsSet());
        entries.addAll(Bennu.getInstance().getEntriesSet());
        details.addAll(Bennu.getInstance().getAccountingTransactionDetailsSet());
        exemptions.addAll(Bennu.getInstance().getExemptionsSet());
        discounts.addAll(Bennu.getInstance().getDiscountsSet());
        DateTime end = new DateTime();
        taskLog("finished warmup, took %d secs%n", new Period(start, end).getSeconds());
    }

    private ExecutionYear executionYearOf(final Event event) {
        return event instanceof AnnualEvent ? ((AnnualEvent) event).getExecutionYear() : ExecutionYear
                .readByDateTime(event.getWhenOccured());
    }

}