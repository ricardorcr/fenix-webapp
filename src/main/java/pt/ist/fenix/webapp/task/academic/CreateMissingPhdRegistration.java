package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.LocalDate;
import pt.ist.fenixframework.FenixFramework;

import java.util.List;


public class CreateMissingPhdRegistration extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final PhdIndividualProgramProcess phd = FenixFramework.getDomainObject("4608500170025");
        final LocalDate date = phd.getWhenStartedStudies();
        final ExecutionYear executionYear = ExecutionYear.readByDateTime(date);
        List<DegreeCurricularPlan> dcps = phd.getPhdProgram().getDegree().getDegreeCurricularPlansForYear(executionYear);
        final Registration registration = new Registration(phd.getPerson(), dcps.iterator().next(),
                RegistrationProtocol.getRegular(), CycleType.THIRD_CYCLE, executionYear);
        registration.setHomologationDate(phd.getCandidacyProcess().getWhenRatified());
        registration.setStudiesStartDate(phd.getCandidacyProcess().getWhenStartedStudies());
        registration.setIngressionType(IngressionType.findByPredicate(IngressionType::isInternal3rdCycleAccess).orElse(null));
        registration.setPhdIndividualProgramProcess(phd);
    }
}
