package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;

public class CheckMaxDatePaymentsAfterLastYearTask extends CustomTask {
    private final DateTime when = new DateTime(2017, 12, 31, 23, 59, 59, 999);

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    Spreadsheet spreadsheet = new Spreadsheet("Enviado para o GIAF em 2018");

    @Override
    public void runTask() throws Exception {
//        warmup();
//        ExecutionYear ex = ExecutionYear.readExecutionYearByName("2012/2013");
//        Multimap<ExecutionYear, Event> eventsByYear = HashMultimap.create();
//        Map<ExecutionYear, Future<Long>> futures = new HashMap<>();
//
//
//        Bennu.getInstance().getAccountingEventsSet().stream().filter(e -> !e.isCancelled())
//                .filter(e -> executionYearOf(e).isAfter(ex) && e.getWhenOccured().isBefore(when))
//                .filter(e -> !e.getNonAdjustingTransactions().isEmpty()).forEach(e -> {
//                    eventsByYear.put(executionYearOf(e), e);
//                });
//
//
//        ExecutorService executorService = Executors.newFixedThreadPool(4);
//
//        eventsByYear.keySet().forEach(e -> {
//            Callable<Long> task = () -> FenixFramework.getTransactionManager().withTransaction(
//                    (CallableWithoutException<Long>) () -> process(e, eventsByYear.get(e), when),
//                    new AtomicInstance(Atomic.TxMode.READ, false));
//            futures.put(e, executorService.submit(task));
//
//        });
//
//        executorService.shutdown();
//        if (executorService.awaitTermination(90, TimeUnit.MINUTES)) {
////            output("log.txt", builder.toString().getBytes());
//        }
//
//        futures.keySet().stream().sorted(ExecutionYear.COMPARATOR_BY_YEAR).forEach(year -> {
//            Future<Long> moneyFuture = futures.get(year);
//            try {
//                Long date = moneyFuture.get();
//                taskLog("Year: %s Total: %s%n", year.getQualifiedName(), date.toString());
//            } catch (InterruptedException | ExecutionException e) {
//                throw new Error(e);
//            }
//        });
//
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        spreadsheet.exportToXLSXSheet(baos);
//        output("pagamentos_2018_giaf.xlsx", baos.toByteArray());
    }

//    private long process(ExecutionYear year, Collection<Event> events, DateTime when) {
//        taskLog("Start for %s%n", year.getQualifiedName());
//
//        events.stream().flatMap(e -> e.getNonAdjustingTransactions().stream())
//                .filter(t -> t.getWhenProcessed().isAfter(when) && t.getWhenRegistered().isBefore(when))
//                .filter(t -> checkIfSentToGiaf(t.getTransactionDetail())).forEach(this::report);
////             .sorted(Comparator.comparing(Event::getExternalId))
////                .max(Comparator.comparing(AccountingTransaction::getWhenProcessed)).map(AccountingTransaction::getWhenProcessed)
////                .orElse(new DateTime());
//
//        return 1;
//    }
//
//    private void report(AccountingTransaction detail) {
//
//        Row row = spreadsheet.addRow();
//        setHeader(row, detail.getEvent());
//        row.setCell("Id Pagamento", detail.getExternalId());
//        row.setCell("Data de Registo no Fenix", detail.getWhenProcessed().toString("dd/MM/yyyy HH:mm:ss"));
//        row.setCell("Data efectiva do pagamento", detail.getWhenRegistered().toString("dd/MM/yyyy HH:mm:ss"));
//        row.setCell("Modo de pagamento", detail.getPaymentMode().toString());
//        row.setCell("Comentário", detail.getComments());
//        row.setCell("Valor do pagamento", detail.getAmountWithAdjustment().toPlainString());
//        row.setCell("Enviado para o GIAF", Boolean.toString(checkIfSentToGiaf(detail.getTransactionDetail())));
//    }
//
//    private void setHeader(Row row, Event event) {
//        LabelFormatter description = event.getDescription();
//        String studentName = getStudentName(event);
//        String gratuityExecutionYearName = getGratuityExecutionYearName(event);
//        String degreeName = getDegreeName(event);
//        String degreeTypeName = getDegreeTypeName(event);
//        String whenOccured = getWhenOccured(event);
//
//        row.setCell("Id", event.getExternalId());
//        row.setCell("Contribuinte", getSocialSecurityNumber(event));
//        row.setCell("Numero", getStudentNumber(event));
//        row.setCell("Utilizador", getUsername(event));
//        row.setCell("Nome", studentName);
//        row.setCell("Ano", gratuityExecutionYearName);
//        row.setCell("Tipo de Curso", degreeTypeName);
//        row.setCell("Curso", degreeName);
//        row.setCell("Descrição", description.toString());
//        row.setCell("Data de criação", whenOccured);
//    }
//
//    String getStudentNumber(Event event) {
//        if (event.getParty() instanceof Person) {
//            if (event.getPerson().getStudent() != null) {
//                return event.getPerson().getStudent().getNumber().toString();
//            }
//        }
//        return "-";
//    }
//
//    String getUsername(Event event) {
//        return event.getParty() instanceof Person ? event.getPerson().getUsername() : "-";
//    }
//
//    String getStudentName(Event event) {
//        return event.getParty() instanceof Person ? event.getPerson().getName() : "Unidade-" + event.getParty().getName();
//    }
//
//    String getSocialSecurityNumber(Event event) {
//        return event.getParty() instanceof Person ? event.getPerson().getSocialSecurityNumber() : event.getParty()
//                .getSocialSecurityNumber();
//    }
//
//    String getGratuityExecutionYearName(Event event) {
//        if (event instanceof AnnualEvent) {
//            return ((AnnualEvent) event).getExecutionYear().getName();
//        }
//        return event.getWhenOccured().toString("yyyy");
//    }
//
//    String getDegreeName(Event event) {
//        if (event instanceof GratuityEvent) {
//            return ((GratuityEvent) event).getDegree().getNameI18N().getContent();
//        }
//        return "N/A";
//    }
//
//    String getDegreeTypeName(Event event) {
//        if (event instanceof GratuityEvent) {
//            return ((GratuityEvent) event).getDegree().getDegreeType().getName().getContent();
//        }
//        return "N/A";
//    }
//
//    String getWhenOccured(Event event) {
//        return event.getWhenOccured().toString("dd/MM/yyyy");
//    }
//
//    private boolean checkIfSentToGiaf(AccountingTransactionDetail detail) {
//        GiafEvent giafEvent = new GiafEvent(detail.getEvent());
//        return giafEvent.hasPayment(detail);
//    }
//
//    private Set<Party> parties = new HashSet<>();
//    private Set<Event> events = new HashSet<>();
//    private Set<AccountingTransaction> transactions = new HashSet<>();
//    private Set<Entry> entries = new HashSet<>();
//    private Set<AccountingTransactionDetail> details = new HashSet<>();
//    private Set<Exemption> exemptions = new HashSet<>();
//    private Set<Discount> discounts = new HashSet<>();
//
//    protected void warmup() {
//        DateTime start = new DateTime();
//        taskLog("started warmup");
//        parties.addAll(Bennu.getInstance().getPartysSet());
//        events.addAll(Bennu.getInstance().getAccountingEventsSet());
//        transactions.addAll(Bennu.getInstance().getAccountingTransactionsSet());
//        entries.addAll(Bennu.getInstance().getEntriesSet());
//        details.addAll(Bennu.getInstance().getAccountingTransactionDetailsSet());
//        exemptions.addAll(Bennu.getInstance().getExemptionsSet());
//        discounts.addAll(Bennu.getInstance().getDiscountsSet());
//        DateTime end = new DateTime();
//        taskLog("finished warmup, took %d secs%n", new Period(start, end).getSeconds());
//    }
//
//    private ExecutionYear executionYearOf(final Event event) {
//        return event instanceof AnnualEvent ? ((AnnualEvent) event).getExecutionYear() : ExecutionYear
//                .readByDateTime(event.getWhenOccured());
//    }

}