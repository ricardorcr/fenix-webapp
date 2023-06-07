package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.IBAN;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.messaging.core.domain.Message;
import pt.ist.fenixframework.FenixFramework;

public class CorrectIBANPayment extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final AccountingTransaction transaction = FenixFramework.getDomainObject("1971845255522088");
        final IBAN iban = transaction.getEvent().getIBAN();
        final Person person = transaction.getEvent().getPerson();
        final Event originalEvent = transaction.getEvent();

        final Event correctEvent = FenixFramework.getDomainObject("1975341358779019");
        if (correctEvent.getIBAN() != null) {
            correctEvent.getIBAN().delete();
        }
        correctEvent.setIBAN(iban);
        transaction.setEvent(correctEvent);

        Message.fromSystem()
                .singleTos(person.getEmailForSendingEmails())
                .subject("Correcção de pagamento por transferência bancária")
                .textBody("Caro " + person.getName() + ",\n\n"
                        + "Foi feito um pagamento através de transferência bancária na sua dívida " + originalEvent.getDescription().toString()
                        + " erradamente, o IBAN gerado foi utilizado por outra pessoa, desta forma o pagamento de 60€ foi removido."
                        + " Se quiser efectuar pagamentos por transferência bancária irá obter um novo IBAN na mesma interface de pagamento.\n\n"
                        + "A Equipa FenixEdu")
                .send();
    }
}
