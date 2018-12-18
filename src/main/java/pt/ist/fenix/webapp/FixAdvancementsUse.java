package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.Exemption;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.calculator.Payment;
import org.fenixedu.academic.domain.accounting.events.EventExemption;
import org.fenixedu.academic.domain.accounting.events.EventExemptionJustificationType;
import org.fenixedu.academic.domain.accounting.events.gratuity.exemption.penalty.FixedAmountFineExemption;
import org.fenixedu.academic.domain.accounting.events.gratuity.exemption.penalty.FixedAmountInterestExemption;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FixAdvancementsUse extends CustomTask {

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        Map<String, String> eventATsMap = new HashMap<>();
        eventATsMap.put("1127420325310818", "1125925676649851");
        eventATsMap.put("1127420325310816", "281500746548063");
        eventATsMap.put("1127420325310815", "1978579764117841");
        eventATsMap.put("1127420325310814", "1978579764117841");
        eventATsMap.put("845945348614533", "1125925676651199");
        eventATsMap.put("845945348614531", "571204880564843");
        eventATsMap.put("845945348614530", "571204880564843");
        eventATsMap.put("845945348614525", "281500746546371");
        eventATsMap.put("845945348614523", "1125925676651848");
        eventATsMap.put("845945348614522", "281500746546988");
        eventATsMap.put("845945348614518", "281500746548243");
        eventATsMap.put("845945348614517", "1131066752501258");
        eventATsMap.put("845945348614513", "1407533797355363");
        eventATsMap.put("845945348614512", "1407533797355363");
        eventATsMap.put("845945348614511", "281500746547498");
        eventATsMap.put("845945348614510", "1125925676650515");
        eventATsMap.put("282995395180452", "281500746548242");
        eventATsMap.put("282995395180451", "1125925676652397");
        eventATsMap.put("282995395180449", "1125925676652510");
        eventATsMap.put("282995395180448", "281500746548340");
        eventATsMap.put("282995395180447", "1126058820636492");
        eventATsMap.put("282995395180446", "1134124769218400");
        eventATsMap.put("282995395180445", "1125925676649915");
        eventATsMap.put("282995395180444", "1125925676650075");
        eventATsMap.put("845945348614516", "1415599745936146");
        eventATsMap.put("845945348614515", "1415599745936146");
        eventATsMap.put("845945348614514", "1415599745936146");

        eventATsMap.forEach((k, v) -> fixAdvancementUse(v, k));
    }

    private void fixAdvancementUse(final String eventID, final String atID) {
        FenixFramework.atomic(() -> {
            Event event = FenixFramework.getDomainObject(eventID);
            AccountingTransaction atAdvancement = FenixFramework.getDomainObject(atID);
            final DateTime now = new DateTime();

            final DebtInterestCalculator debtInterestCalculator = event.getDebtInterestCalculator(now);
            final Optional<Payment> paymentById = debtInterestCalculator.getPaymentById(atID);
            if (!paymentById.isPresent()) {
                taskLog("N'as pas de pagamento! %s %s%n", eventID, atID);
                return;
            }
            final Money usedInDebt = new Money(paymentById.get().getUsedAmountInDebts());
            final Money usedInInterest = new Money(paymentById.get().getUsedAmountInInterests());
            final Money usedInFine = new Money(paymentById.get().getUsedAmountInFines());

            final Set<AccountingTransaction> eventATs = new HashSet<>();
            event.getAccountingTransactionsSet().stream()
                    .filter(at -> at != atAdvancement)
                    .forEach(at -> {
                        eventATs.add(at);
                        at.setEvent(null);
                    });

            final Set<Exemption> eventExemptions = new HashSet<>();
            eventExemptions.addAll(event.getExemptionsSet());
            event.getExemptionsSet().forEach(ex -> ex.setEvent(null));

            atAdvancement.getRefund().delete();
            String reason = "Utilização indevida de adiantamento por má configuração da tabela de juros.";
            Person responsible = User.findByUsername("ist24616").getPerson();

            if (usedInDebt.isPositive()) {
                final EventExemption eventExemption = new EventExemption(event, responsible, usedInDebt,
                        EventExemptionJustificationType.DIRECTIVE_COUNCIL_AUTHORIZATION, now, reason);
                eventExemption.setWhenCreated(atAdvancement.getWhenRegistered());
            }
            if (usedInInterest.isPositive()) {
                final FixedAmountInterestExemption interestExemption = new FixedAmountInterestExemption(event, responsible, usedInInterest,
                        EventExemptionJustificationType.DIRECTIVE_COUNCIL_AUTHORIZATION, now, reason);
                interestExemption.setWhenCreated(atAdvancement.getWhenRegistered());
            }
            if (usedInFine.isPositive()) {
                final FixedAmountFineExemption fineExemption = new FixedAmountFineExemption(event, responsible, usedInFine,
                        EventExemptionJustificationType.DIRECTIVE_COUNCIL_AUTHORIZATION, now, reason);
                fineExemption.setWhenCreated(atAdvancement.getWhenRegistered());
            }

            eventATs.forEach(at -> at.setEvent(event));
            eventExemptions.forEach(ex -> ex.setEvent(event));
            event.recalculateState(new DateTime());
        });
        taskLog("Processed event %s tx %s%n", eventID, atID);
    }
}
