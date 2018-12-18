package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.candidacyProcess.secondCycle.SecondCycleIndividualCandidacy;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;

public class Check2ndCycleCandidacies extends CustomTask {

    @Override
    public void runTask() throws Exception {

        DateTime periodStart = new DateTime(2019, 9, 23, 0, 0);
        Bennu.getInstance().getIndividualCandidaciesSet().stream()
                .filter(c -> !c.isCancelled())
                .filter(SecondCycleIndividualCandidacy.class::isInstance)
                .map(c -> (SecondCycleIndividualCandidacy) c)
                .filter(sc -> sc.getWhenCreated().isAfter(periodStart))
                .filter(sc ->
                {
                    Money whatShouldBe = new Money(sc.getSelectedDegreesSet().size() * 100.00);
                    if (!whatShouldBe.equals(sc.getEvent().getOriginalAmountToPay())) {
                        return true;
                    } else {
                        return false;
                    }
                })
                .forEach(sc ->
                {
                    Money whatShouldBe = new Money(sc.getSelectedDegreesSet().size() * 100.00);
                    if (whatShouldBe.greaterThan(sc.getEvent().getOriginalAmountToPay())) {
                        taskLog("Dívida com valor inferior -> inscreveu-se a mais cursos: Valor Cursos: %s Valor Evento: %s %s - Candidatura: %s - Evento: %s %n",
                                new Money(sc.getSelectedDegreesSet().size() * 100.00), sc.getEvent().getOriginalAmountToPay(),
                                sc.getPersonalDetails().getName(), sc.getExternalId(), sc.getEvent().getExternalId());
                    } else {
                        taskLog("Dívida com valor superior -> desinscreveu-se de curso(s): Valor Cursos: %s Valor Evento: %s %s - Candidatura: %s - Evento: %s %n",
                                new Money(sc.getSelectedDegreesSet().size() * 100.00), sc.getEvent().getOriginalAmountToPay(),
                                sc.getPersonalDetails().getName(), sc.getExternalId(), sc.getEvent().getExternalId());
                    }
                });
    }
}