package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.calculator.Payment;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.joda.time.DateTime;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;

public class CheckTransactionInterestSapRequest extends ReadCustomTask {

    int count = 0;
    @Override
    public void runTask() throws Exception {
        final DateTime now = new DateTime();
        final Spreadsheet spreadsheet = new Spreadsheet("Casos");
        Bennu.getInstance().getAccountingEventsSet().stream()
                .filter(event -> !event.getSapRequestSet().isEmpty())
                .filter(event -> !event.getAccountingTransactionsSet().isEmpty())
                .forEach(event -> {
                    DebtInterestCalculator calculator = event.getDebtInterestCalculator(now);
                    calculator.getPayments()
                            .filter(Payment::isForInterest)
                            .forEach(payment -> {
                                SapRequest sapRequest = event.getSapRequestSet().stream()
                                        .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT_INTEREST)
                                        .filter(sr -> sr.getPayment() != null && sr.getPayment().getExternalId().equals(payment.getId()))
                                        .findAny().orElse(null);
                                //TODO ver na bd casos juros sem tx's e detectar padrao para fazer if
                                if (sapRequest == null) {
//                                    Spreadsheet.Row row = spreadsheet.addRow();
//                                    row.setCell("Event", event.getExternalId());
//                                    row.setCell("Tx", payment.getId());
//                                    row.setCell("Value", payment.getAmount());
//                                    row.setCell("Date", payment.getDate().toString("dd/MM/yyyy HH:mm:ss"));
                                    count++;
                                }
                            });
                });
//        output("wrong_interests.xlsx", spreadsheet.exportToXLSXSheet());
        taskLog("Vamos ver %s%n", count);
    }
}
