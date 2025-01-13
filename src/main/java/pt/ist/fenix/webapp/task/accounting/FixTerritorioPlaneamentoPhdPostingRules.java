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
        final List<String> eventIDs = Arrays.asList("2539523967811861", "2539523967811662", "1976574014391554", "3100803178954757", "1976574014391229", "1976574014391049", "2820998944522470",
                "2820998944522458", "2820998944522289", "2258048991101137", "2258048991101011", "2258048991101000", "2256730436141102", "2256378248822796", "1412305506009379", "1693780482720138",
                "1693780482720137", "1975255459430716", "1695099037680922", "1695099037680833", "1695099037680796", "1413624060970040", "1413624060969980", "1132149084260225", "1130830529298790",
                "1130478341980639", "850674107549740", "850674107549389", "850674107549341", "850674107548783", "569199130838792", "569199130838734", "569199130838731", "569199130838563",
                "287724154128044", "287724154128014", "287724154127915", "849355552588247", "849355552588094", "849003365270190", "849003365270181", "849003365270025", "567880575877509",
                "567880575877506", "567880575877502", "567880575877474", "567528388559631", "567528388559587", "567528388559574", "567528388559548", "567528388559449", "286053411848781");


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
