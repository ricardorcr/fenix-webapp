package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.candidacyProcess.mobility.MobilityProgram;
import org.fenixedu.academic.domain.mobility.outbound.OutboundMobilityCandidacyPeriod;
import org.fenixedu.academic.domain.organizationalStructure.UniversityUnit;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class AddOutboundCandidacyDegree extends CustomTask {

    @Override
    public void runTask() throws Exception {
        OutboundMobilityCandidacyPeriod period = FenixFramework.getDomainObject("1977145245040645");

        ExecutionDegree megie = ExecutionDegree.getByDegreeCurricularPlanNameAndExecutionYear(
                "MEGIE 2019", ExecutionYear.readCurrentExecutionYear());
        MobilityProgram erasmus = FenixFramework.getDomainObject("6489695584257");
        UniversityUnit kth = FenixFramework.getDomainObject("281582350893142");
        period.createOutboundMobilityCandidacyContest(megie, erasmus, kth, Integer.valueOf(1));
    }
}
