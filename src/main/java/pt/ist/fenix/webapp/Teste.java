package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class Teste extends CustomTask {

    @Override
    public void runTask() throws Exception {
        fix("2541881904863015", "�", "É"); //VIRIATO JOS� FERNANDES
        fix("1415981998018009", "�", "Ö"); //ONNI KIVISTÖ
        fix("1415981998016673", "�", "É"); //JOS� GAMBOA CHAVES DA F
        fix("2541881904860842", "M�", "MË"); //M�NICA DA CONCEI�
        fix("2541881904860842", "MÓNICA", "MËNICA"); //M�NICA DA CONCEI�
        fix("2541881904860842", "I�", "IÃ"); //M�NICA DA CONCEI�
        fix("2823356881570120", "�", "ß"); //Oskar Borgvall Gonzß
    }

    private void fix(final String oid, final String oldChar, final String newChar) {
        final Event event = FenixFramework.getDomainObject(oid);
        event.getAdjustedTransactions().stream()
                .filter(tx -> tx.getIBANPayment() != null)
                .forEach(tx -> {
                    String comments = tx.getTransactionDetail().getComments();
                    tx.getTransactionDetail().setComments(comments.replace(oldChar, newChar));
                    String settlement = tx.getIBANPayment().getSettlement();
                    tx.getIBANPayment().setSettlement(settlement.replace(oldChar, newChar));
                });
    }
}
