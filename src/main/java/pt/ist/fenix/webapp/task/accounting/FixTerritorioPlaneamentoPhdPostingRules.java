package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.PostingRule;
import org.fenixedu.academic.domain.accounting.postingRules.FixedAmountPR;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.phd.PhdProgram;
import org.fenixedu.academic.domain.phd.debts.PhdGratuityPR;
import org.fenixedu.academic.domain.phd.debts.PhdRegistrationFeePR;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;
import java.util.List;

public class FixTerritorioPlaneamentoPhdPostingRules extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final List<String> eventIDs = Arrays.asList("1413624060969453","1132149084259771","569199130838271","567880575877381","286053411848591");


        final PhdProgram territorio = FenixFramework.getDomainObject("4604204947716");
        final PhdProgram territorioPlaneamento = FenixFramework.getDomainObject("2256404018626561");

        eventIDs.stream()
                .map(eventID -> (Event) FenixFramework.getDomainObject(eventID))
                .forEach(event -> {
                    PostingRule oldPR = territorio.getServiceAgreementTemplate().findPostingRuleByEventTypeAndDate(event.getEventType(), event.getWhenOccured());
                    PostingRule newPR = territorioPlaneamento.getServiceAgreementTemplate().findPostingRuleByEventTypeAndDate(event.getEventType(), new DateTime());
                    boolean somethingToDo = false;
                    try {
                        territorioPlaneamento.getServiceAgreementTemplate().findPostingRuleByEventTypeAndDate(event.getEventType(), event.getWhenOccured());
                    } catch (DomainException de) {
                        somethingToDo = true;
                    }
                    if (somethingToDo) {
                        try {
                            if (newPR != null) {
                                newPR.setStartDate(event.getWhenOccured().minusDays(1));
                            } else if (oldPR instanceof PhdRegistrationFeePR oldRegistrationFeePR) {
                                new PhdRegistrationFeePR(event.getWhenOccured().minus(1), null, territorioPlaneamento.getServiceAgreementTemplate(),
                                        (oldRegistrationFeePR).getFixedAmount(), (oldRegistrationFeePR).getFixedAmountPenalty());
                            } else if (oldPR instanceof PhdGratuityPR oldGratuityPR) {
                                new PhdGratuityPR(event.getWhenOccured().minusDays(1), null, territorioPlaneamento.getServiceAgreementTemplate(),
                                        oldGratuityPR.getGratuity(), oldGratuityPR.getFineRate());
                            } else if (oldPR instanceof FixedAmountPR oldFixedAmountPR) {
                                new FixedAmountPR(oldFixedAmountPR.getEntryType(), event.getEventType(), event.getWhenOccured().minusDays(1), null,
                                        territorioPlaneamento.getServiceAgreementTemplate(), oldFixedAmountPR.getFixedAmount());
                            } else {
                                taskLog("Hummmm: %s - %s%n", event.getExternalId(), oldPR.getClass().getName());
                            }
                        } catch (Exception e) {
                            taskLog("Evento: %s\t%s%n", event.getExternalId(), oldPR.getClass().getName());
                            throw e;
                        }
                    }
                });

    }
}
