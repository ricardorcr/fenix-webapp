package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.AccountingTransaction_Base;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.IBANPayment;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.bennu.search.domain.DomainIndexSystem;
import org.fenixedu.bennu.search.domain.KeyIndex;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class UpdateIBANSettlementIndex extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final List<String> eventIDs = Arrays.asList("2541881904863015","1415981998018009","1415981998016673","2541881904860842","2541881904860842","2823356881570120");
        eventIDs.stream()
                .map(eventID -> (Event) FenixFramework.getDomainObject(eventID))
                .flatMap(event -> event.getAdjustedTransactions().stream().map(AccountingTransaction::getIBANPayment))
                .filter(Objects::nonNull)
                .forEach(ibanPayment -> DomainIndexSystem.getInstance().index(ibanPayment.getSettlement(), KeyIndex::getIBANPaymentSet, ibanPayment));
    }
}
