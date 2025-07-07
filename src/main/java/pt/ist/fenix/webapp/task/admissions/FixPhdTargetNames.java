package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOffice;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.phd.PhdProgram;
import org.fenixedu.academic.domain.phd.PhdProgramContextPeriod;
import org.fenixedu.academic.domain.phd.PhdProgramUnit;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;

public class FixPhdTargetNames extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final AdmissionProcess admissionProcess = FenixFramework.getDomainObject("3104707304226878");
        Arrays.asList("3104827563315837", "3104827563315840", "3104827563315848").forEach(targetId -> {
            final AdmissionProcessTarget target = FenixFramework.getDomainObject(targetId);
            ExecutionYear nextExecutionYear = ExecutionYear.readCurrentExecutionYear().getNextExecutionYear();
            final String degreeId = target.getOutcomeConfigJson().get("degree").getAsString();
            final Degree degree = FenixFramework.getDomainObject(degreeId);
            final LocalizedString name = BundleUtil.getLocalizedString(Bundle.PHD, "label.php.program")
                    .append(" ")
                    .append(BundleUtil.getString(Bundle.APPLICATION, "label.in"))
                    .append(" ")
                    .append(degree.getNameI18N(nextExecutionYear));
            target.setName(name);
        });

        final Degree degree = FenixFramework.getDomainObject("1128661570813996"); //Tecnologias e Ciências Nucleares;
        createPhdProgram(degree);
        admissionProcess.createAdmissionProcessTarget(degree.getNameI18N(), 5);
    }

    private PhdProgram createPhdProgram(final Degree degree) {
        final ExecutionSemester executionSemester = ExecutionSemester.readActualExecutionSemester().getNextExecutionPeriod(); //make sure it is the correct date!!
        final AdministrativeOffice office = FenixFramework.getDomainObject("2461016260609");
        final PhdProgram phdProgram = PhdProgram.create(degree, degree.getNameI18N(), degree.getSigla());
        phdProgram.setAdministrativeOffice(office);
        PhdProgramContextPeriod.create(phdProgram,executionSemester.getBeginDateYearMonthDay().toDateTimeAtMidnight(), null);

        final Unit parent = FenixFramework.getDomainObject("1262720937198");
        PhdProgramUnit.create(phdProgram, phdProgram.getName(), phdProgram.getWhenCreated().toYearMonthDay(), null, parent);
        return phdProgram;
    }
}
