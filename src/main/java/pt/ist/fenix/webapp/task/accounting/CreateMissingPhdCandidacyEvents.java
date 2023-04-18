package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.phd.candidacy.PHDProgramCandidacy;
import org.fenixedu.academic.domain.phd.candidacy.PhdProgramCandidacyEvent;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class CreateMissingPhdCandidacyEvents extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getCandidaciesSet().stream()
                .filter(PHDProgramCandidacy.class::isInstance)
                .map(candidacy -> (PHDProgramCandidacy) candidacy)
                .filter(candidacy -> candidacy.getCandidacyProcess() != null && candidacy.getCandidacyProcess().getPhdProgram() != null)
                .filter(candidacy -> candidacy.getCandidacyProcess().getPhdProgram().getAcronym().equals("PDMD"))
                .filter(candidacy -> candidacy.getCandidacyProcess().getEvent() == null)
                .forEach(candidacy -> {
                    try {
                        taskLog("Vou criar evento para: %s\t%s%n", candidacy.getPerson().getName(), candidacy.getExternalId());
                        new PhdProgramCandidacyEvent(candidacy.getPerson(), candidacy.getCandidacyProcess());
                    } catch (Throwable e) {
                        taskLog("APANHEI!!");
                        e.printStackTrace();
                    }
                });

//        throw new Error("Test Drive");
    }
}
