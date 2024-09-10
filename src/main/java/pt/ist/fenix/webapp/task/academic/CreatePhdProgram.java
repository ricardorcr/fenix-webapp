package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.accounting.ServiceAgreementTemplate;
import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOffice;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.phd.PhdProgram;
import org.fenixedu.academic.domain.phd.PhdProgramContextPeriod;
import org.fenixedu.academic.domain.phd.PhdProgramUnit;
import org.fenixedu.academic.domain.phd.candidacy.PhdProgramCandidacyPR;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;
import pt.ist.fenixframework.FenixFramework;

import java.lang.reflect.Method;

public class CreatePhdProgram extends WriteCustomTask {

    @Override
    public void runTask() throws Exception {
        final ExecutionSemester actualExecutionSemester = ExecutionSemester.readActualExecutionSemester(); //make sure it is the correct date!!
        final Degree degree = FenixFramework.getDomainObject("2761663971761");
        final AdministrativeOffice office = FenixFramework.getDomainObject("2461016260609");
        final PhdProgram phdProgram = PhdProgram.create(degree, degree.getNameI18N(), degree.getSigla());
        phdProgram.setAdministrativeOffice(office);
        PhdProgramContextPeriod.create(phdProgram,actualExecutionSemester.getBeginDateYearMonthDay().toDateTimeAtMidnight(), null);

        final Unit parent = FenixFramework.getDomainObject("1262720937198");
        PhdProgramUnit.create(phdProgram, phdProgram.getName(), phdProgram.getWhenCreated().toYearMonthDay(), null, parent);

        final ServiceAgreementTemplate serviceAgreementTemplate = degree.getLastActiveDegreeCurricularPlan().getServiceAgreementTemplate();
        new PhdProgramCandidacyPR(serviceAgreementTemplate, new DateTime(2023,9,1,0,0), null, new Money(100));
    }

}