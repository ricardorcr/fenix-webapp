package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.PaymentMethod;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class FixDuplicateIBANTransactions extends SapCustomTask {

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    @Override
    protected void runTask(ErrorLogConsumer errorLogConsumer, EventLogger eventLogger) {
        final PaymentMethod dedicatedIban = FenixFramework.getDomainObject("571239240302594");
        final Set<Event> possibleProblems = new HashSet<>();
        try {
            FenixFramework.atomic(() -> dedicatedIban.setAllowManualUse(true));
            final Spreadsheet canceledTransactions = new Spreadsheet("Transactions");
            final Spreadsheet canceledSapRequests = canceledTransactions.addSpreadsheet("SapRequests");
            final Spreadsheet errors = canceledSapRequests.addSpreadsheet("Errors");
            Bennu.getInstance().getIBANGroupSet().stream()
                    .flatMap(ibanGroup -> ibanGroup.getIBANSet().stream())
                    .flatMap(iban -> iban.getIBANPaymentSet().stream())
                    .filter(ibanPayment -> ibanPayment.getSettlement() != null && ibanPayment.getSettlement().startsWith("XXX"))
                    .forEach(ibanPayment -> {
                        final AccountingTransaction accountingTransaction = ibanPayment.getAccountingTransaction();
                        try {
                            final Spreadsheet.Row row = canceledTransactions.addRow();
                            final Set<SapRequest> sapRequestSet = accountingTransaction.getSapRequestSet();
                            if (!sapRequestSet.isEmpty()) {
                                FenixFramework.atomic(() -> {
                                    SapRequest lastEventRequest =
                                            accountingTransaction.getEvent().getSapRequestSet().stream()
                                            .max(Comparator.comparing(SapRequest::getOrder)).get();
                                    if (lastEventRequest != sapRequestSet.stream().max(Comparator.comparing(SapRequest::getOrder)).get()) {
                                        possibleProblems.add(accountingTransaction.getEvent());
                                    } else {
                                        for (SapRequest sapRequest : sapRequestSet) {
                                            final SapEvent sapEvent = new SapEvent(sapRequest.getEvent());
                                            sapEvent.cancelDocument(sapRequest);
                                            Spreadsheet.Row sapRow = canceledSapRequests.addRow();
                                            sapRow.setCell("Event OID", accountingTransaction.getEvent().getExternalId());
                                            sapRow.setCell("Request OID", sapRequest.getExternalId());
                                            sapRow.setCell("Type", sapRequest.getRequestType().toString());
                                            sapRow.setCell("Value", sapRequest.getValue().toString());
                                            sapRow.setCell("Advancement", sapRequest.getAdvancement().toString());
                                            sapEvent.processPendingRequests(accountingTransaction.getEvent(), errorLogConsumer, eventLogger);
                                        }
                                        accountingTransaction.annul(User.findByUsername("ist24616"), "Transação IBAN " +
                                                "duplicada");
                                        row.setCell("Event OID", accountingTransaction.getEvent().getExternalId());
                                        row.setCell("Tx OID", accountingTransaction.getExternalId());
                                        row.setCell("Value", accountingTransaction.getOriginalAmount().toString());
                                        row.setCell("Settlement", ibanPayment.getSettlement());
                                    }
                                });
                            } else {
                                FenixFramework.atomic(() -> {
                                    accountingTransaction.annul(User.findByUsername("ist24616"), "Transação IBAN " +
                                            "duplicada");
                                    row.setCell("Event OID", accountingTransaction.getEvent().getExternalId());
                                    row.setCell("Tx OID", accountingTransaction.getExternalId());
                                    row.setCell("Value", accountingTransaction.getOriginalAmount().toString());
                                    row.setCell("Settlement", ibanPayment.getSettlement());
                                });
                            }
                        } catch (Exception e) {
                            Spreadsheet.Row row = errors.addRow();
                            row.setCell("Event OID", accountingTransaction.getEvent().getExternalId());
                            row.setCell("Tx OID", accountingTransaction.getExternalId());
                            row.setCell("Value", accountingTransaction.getOriginalAmount().toString());
                            row.setCell("Error", e.getMessage());
                            e.printStackTrace();
                        }
                    });

            possibleProblems.forEach(event -> taskLog("Houston, we may have a problem: %s%n", event.getExternalId()));
            output("duplicate_ibans_errors.xlsx", canceledTransactions.exportToXLSXSheet());
        } finally {
            FenixFramework.atomic(() -> dedicatedIban.setAllowManualUse(false));
        }
//        throw new Error("Dry run!");
    }
}
