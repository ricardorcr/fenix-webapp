package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.HashSet;
import java.util.Set;

public class FixWrongAdvancementsUse extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final User responsible = User.findByUsername("ist24616");
        fix(responsible, "571557067892220", "564470371920800");
        fix(responsible, "571557067892221", "564470371920799");
        fix(responsible, "571557067892222", "564470371920798");
    }

    private void fix(final User responsible, final String eventID, final String txID) {
        Event event = FenixFramework.getDomainObject(eventID);
        AccountingTransaction txToCancel = FenixFramework.getDomainObject(txID);
        Set<AccountingTransaction> toKeep = new HashSet<>();
        toKeep.addAll(event.getAccountingTransactionsSet());

        event.getAccountingTransactionsSet().stream()
                .filter(tx -> tx != txToCancel)
                .forEach(tx -> tx.setEvent(null));

        txToCancel.getSapRequestSet().forEach(sr -> sr.setRefund(null));
        txToCancel.annul(responsible, "Utilização indevida de adiantamento");
        for (AccountingTransaction tx : toKeep) {
            tx.setEvent(event);
        }
    }
}
