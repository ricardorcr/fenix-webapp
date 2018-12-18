package pt.ist.fenix.webapp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
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
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.academic.domain.accounting.Discount;
import org.fenixedu.academic.domain.accounting.Entry;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.Exemption;
import org.fenixedu.academic.domain.accounting.calculator.Debt;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.events.AnnualEvent;
import org.fenixedu.academic.domain.accounting.events.PenaltyExemption;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEvent;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.util.LabelFormatter;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.CallableWithoutException;
import pt.ist.fenixframework.FenixFramework;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */
@SuppressWarnings("Duplicates")
public class ListCurrentDebtsTask extends CustomTask {
    private final DateTime when = new DateTime(2017, 12, 31, 23, 59, 59, 999);
    private final String amountToPayWhenHeader = "Valor a " + when.toString("dd-MM-yyyy");

    private StringBuilder builder = new StringBuilder();

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    String getStudentNumber(Event event) {
        if (event.getParty() instanceof Person) {
            if (event.getPerson().getStudent() != null) {
                return event.getPerson().getStudent().getNumber().toString();
            }
        }
        return "-";
    }

    String getUsername(Event event) {
        return event.getParty() instanceof Person ? event.getPerson().getUsername() : "-";
    }

    String getStudentName(Event event) {
        return event.getParty() instanceof Person ? event.getPerson().getName() : "Unidade-" + event.getParty().getName();
    }

    String getSocialSecurityNumber(Event event) {
        return event.getParty() instanceof Person ? event.getPerson().getSocialSecurityNumber() : event.getParty()
                                                                                                       .getSocialSecurityNumber();
    }

    String getGratuityExecutionYearName(Event event) {
        if (event instanceof AnnualEvent) {
            return ((AnnualEvent) event).getExecutionYear().getName();
        }
        return event.getWhenOccured().toString("yyyy");
    }

    String getDegreeName(Event event) {
        if (event instanceof GratuityEvent) {
            return ((GratuityEvent) event).getDegree().getNameI18N().getContent();
        }
        return "N/A";
    }

    String getDegreeTypeName(Event event) {
        if (event instanceof GratuityEvent) {
            return ((GratuityEvent) event).getDegree().getDegreeType().getName().getContent();
        }
        return "N/A";
    }

    String getWhenOccured(Event event) {
        return event.getWhenOccured().toString("dd/MM/yyyy");
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

    private class SpreadsheetGenerator implements Callable<Spreadsheet> {

        private ExecutionYear year;
        private Collection<Event> events;
        private DateTime when;

        public SpreadsheetGenerator(ExecutionYear year, Collection<Event> events, DateTime when) {
            this.year = year;
            this.events = events;
            this.when = when;
        }

        @Override
        public Spreadsheet call() throws Exception {
            return FenixFramework.getTransactionManager().withTransaction(
                (CallableWithoutException<Spreadsheet>) () -> process(this.year, this.events, this.when),
                new AtomicInstance(TxMode.READ,
                                   false));
        }

        public String getFilename() {
            return "dividas_" + year.getQualifiedName().replaceAll("/", "_");
        }
    }

    @Override
    public void runTask() throws Exception {
        warmup();
        log("when: %s%n", when.toString());
        ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("1992/1993");
        LocalDate beginLocalDate = executionYear.getBeginLocalDate();
        log("start date : %s %s%n", beginLocalDate.toString("dd/MM/yyyy"), executionYear.getQualifiedName());
        Multimap<ExecutionYear, Event> eventsByYear = HashMultimap.create();
        Map<ExecutionYear, Future<Spreadsheet>> futures = new HashMap<>();

        Bennu.getInstance().getAccountingEventsSet().stream()
             .filter(e -> !e.isCancelled())
             .filter(e -> isNotBefore(executionYear, e))
//             .sorted(Comparator.comparing(Event::getExternalId))
             .forEach(e -> {
                 eventsByYear.put(executionYearOf(e), e);
             });

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        eventsByYear.keySet().stream().filter(e -> e.getQualifiedName().equals("2014/2015")).forEach(e -> {
            //Callable<Spreadsheet> task = new SpreadsheetGenerator(e, eventsByYear.get(e), when);
            //futures.put(task, executorService.submit(task));
            Callable<Spreadsheet> task =
                () -> FenixFramework
                          .getTransactionManager()
                          .withTransaction((CallableWithoutException<Spreadsheet>) () -> process(e, eventsByYear.get(e), when),
                                           new AtomicInstance(TxMode.READ, false));
            futures.put(e, executorService.submit(task));

        });

        executorService.shutdown();
        if (executorService.awaitTermination(90, TimeUnit.MINUTES)) {
            output("log.txt", builder.toString().getBytes());

        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(baos);

        futures.keySet().stream().sorted(ExecutionYear.COMPARATOR_BY_YEAR).forEach(year -> {
            try (ByteArrayOutputStream tmp = new ByteArrayOutputStream()) {
                String filename = "dividas_fenix_v3/dividas_" + year.getQualifiedName().replaceAll("/", "_") + ".xlsx";
                zipOutputStream.putNextEntry(new ZipEntry(filename));
                Spreadsheet spreadsheet = futures.get(year).get();
                spreadsheet.exportToXLSXSheet(tmp);
                tmp.writeTo(zipOutputStream);
                zipOutputStream.closeEntry();
            } catch (InterruptedException | ExecutionException | IOException e) {
                throw new Error(e);
            }
        });

        zipOutputStream.close();
        output("dividas_fenix.zip", baos.toByteArray());

        taskLog("%s%n", parties.size());
        taskLog("%s%n", events.size());
        taskLog("%s%n", transactions.size());
        taskLog("%s%n", entries.size());
        taskLog("%s%n", details.size());
        taskLog("%s%n", exemptions.size());
        taskLog("%s%n", discounts.size());
    }

    private Spreadsheet process(ExecutionYear e, Collection<Event> events, DateTime when) {
        taskLog("process %s %n", e.getQualifiedName());
        Spreadsheet spreadsheet = new Spreadsheet("dividas_" + e.getQualifiedName().replaceAll("/", "_"));
        events.forEach(event -> {
            reportEvent(when, spreadsheet, event);
        });
        return spreadsheet;
    }

    private void reportEvent(DateTime when, Spreadsheet spreadsheet, Event event) {
        try {

            DebtInterestCalculator debtInterestCalculator = event.getDebtInterestCalculator(when);

            LabelFormatter description = event.getDescription();
            String studentName = getStudentName(event);
            String gratuityExecutionYearName = getGratuityExecutionYearName(event);
            String degreeName = getDegreeName(event);
            String degreeTypeName = getDegreeTypeName(event);
            String whenOccured = getWhenOccured(event);

            Money originalAmountToPay =
                new Money(event.getDebtInterestCalculator(event.getWhenOccured().plusSeconds(1)).getDebtAmount());
            Money amountToPayWhen = new Money(debtInterestCalculator.getDebtAmount());
            Money interestAmount = new Money(debtInterestCalculator.getInterestAmount());
            Money interestAmountToPay = new Money(debtInterestCalculator.getDueInterestAmount());

            Money fineAmount = new Money(debtInterestCalculator.getFineAmount());
            Money fineAmountToPay = new Money(debtInterestCalculator.getDueFineAmount());

            Money amountToPay = new Money(debtInterestCalculator.getDueAmount());
            Money totalPayedAmount = new Money(debtInterestCalculator.getTotalPaidAmount());
            Money totalAmountToPay = new Money(debtInterestCalculator.getTotalAmount());
            Money debtAmountPayed = new Money(debtInterestCalculator.getPaidDebtAmount());
            Money interestAmountPayed = new Money(debtInterestCalculator.getPaidInterestAmount());
            Money fineAmountPayed = new Money(debtInterestCalculator.getPaidFineAmount());
            boolean isFineExempted = debtInterestCalculator.getDebtsOrderedByDueDate().stream().anyMatch(Debt::isToExemptFine);

            Row row = spreadsheet.addRow();

            row.setCell("Id", event.getExternalId());
            row.setCell("Contribuinte", getSocialSecurityNumber(event));
            row.setCell("Numero", getStudentNumber(event));
            row.setCell("Utilizador", getUsername(event));
            row.setCell("Nome", studentName);
            row.setCell("Ano", gratuityExecutionYearName);
            row.setCell("Tipo de Curso", degreeTypeName);
            row.setCell("Curso", degreeName);
            row.setCell("Descrição", description.toString());
            row.setCell("Data de criação", whenOccured);
            row.setCell("Valor Original", originalAmountToPay.toPlainString());
            row.setCell("Valor Juro", interestAmount.toPlainString());
            row.setCell("Valor Multa", fineAmount.toPlainString());
            row.setCell("Valor Total", totalAmountToPay.toPlainString());
            row.setCell("Valor Pago divida", debtAmountPayed.toPlainString());
            row.setCell("Valor Pago juro", interestAmountPayed.toPlainString());
            row.setCell("Valor Pago multa", fineAmountPayed.toPlainString());
            row.setCell("Valor Pago total", totalPayedAmount.toPlainString());
            row.setCell("Valor em divida", amountToPay.toPlainString());
            row.setCell("Juro em divida", interestAmountToPay.toPlainString());
            row.setCell("Multa em divida", fineAmountToPay.toPlainString());
            row.setCell("Desconto", getDiscountsAmount(event, when).toPlainString());
            row.setCell("Valor Isento Divida", getExemptionAmount(event, when, originalAmountToPay).toPlainString());
            row.setCell("Isento Multa", isFineExempted ? "Sim" : "Não");
            row.setCell("Aberto", Boolean.toString(amountToPay.greaterThan(Money.ZERO)));
        } catch (NullPointerException | UnsupportedOperationException | DomainException npe) {
            log("%s %s %s %s%n", event.getExternalId(), event.getClass().getSimpleName(), event.getDescription().toString(),
                org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace
                                                                     (npe));
        }
    }

    private Money getDiscountsAmount(Event event, DateTime when) {
        return event.getDiscountsSet().stream().filter(d -> !d.getWhenCreated().isAfter(when)).map(Discount::getAmount)
                    .reduce(Money.ZERO, Money::add);
    }

    private static String toString(Map<LocalDate, Money> map) {
        return map.entrySet().stream().sorted(Map.Entry.comparingByKey())
                  .map(e -> String.format("%s -> %s", e.getKey(), e.getValue().toPlainString()))
                  .collect(Collectors.joining("\n"));
    }

    private static Map<LocalDate, Money> getPayments(Event event) {
        return event.getSortedNonAdjustingTransactions().stream()
                    .collect(Collectors.toMap(t -> t.getWhenRegistered().toLocalDate(),
                                              AccountingTransaction::getAmountWithAdjustment, Money::add));
    }

    private static String getPaymentsDescription(Event event) {
        return toString(getPayments(event));
    }

    private Money getExemptionAmount(Event event, DateTime when, Money originalAmount) {
        return event.getExemptionsSet()
                    .stream()
                    .filter(e -> !e.getWhenCreated().isAfter(when))
                    .filter(e -> !(e instanceof PenaltyExemption))
                    .map(e -> e.getExemptionAmount(originalAmount))
                    .reduce(Money.ZERO, Money::add);
    }

    private ExecutionYear executionYearOf(final Event event) {
        return event instanceof AnnualEvent ? ((AnnualEvent) event).getExecutionYear() : ExecutionYear.readByDateTime(
            event.getWhenOccured());
    }

    private String computeHostName() {
        return CoreConfiguration.getConfiguration().applicationUrl();
    }

    protected boolean isNotBefore(ExecutionYear year, Event e) {
        return !executionYearOf(e).isBefore(year);
//        return executionYearOf(e).getQualifiedName().equals("2018/2019");
    }

    private void log(String format, Object... args) {
        builder.append(String.format(format, args));
    }
}