package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.joda.time.LocalDate;

public class CheckManualBankTransfers extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {

        final LocalDate startDate = new LocalDate(2023,04,01);
        final long count = Bennu.getInstance().getAccountingTransactionsSet().stream()
                .filter(at -> at.getEvent().getIBAN() != null)
                .filter(at -> at.getPaymentMethod().getDescription().getContent().startsWith("TB"))
                .filter(at -> at.getWhenRegistered().toLocalDate().isAfter(startDate))
                .peek(at -> taskLog("%s\t%s\t%s\t%s%n", at.getWhenRegistered().toString("dd-MM-yyyy"), at.getEvent().getExternalId(), at.getExternalId(), at.getOriginalAmount().toPlainString()))
                .count();

        taskLog("#%s", count);
    }
}
