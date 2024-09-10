package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.GradeScale;
import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOffice;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CurricularStage;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.YearMonthDay;
import pt.ist.fenixframework.FenixFramework;

import static org.fenixedu.academic.util.LocaleUtils.EN;
import static org.fenixedu.academic.util.LocaleUtils.PT;

public class CreateMissingOldDEAs extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final AdministrativeOffice postGraduate = FenixFramework.getDomainObject("2461016260609");
        final YearMonthDay initialDate = new YearMonthDay(1980, 8, 1);
        final YearMonthDay endDate = new YearMonthDay(2005, 8, 1);
        final AcademicPeriod academicPeriod = AcademicPeriod.getAcademicPeriodFromString("org.fenixedu.academic.domain.time.calendarStructure.AcademicYears:2");

        final LocalizedString minasLS = new LocalizedString(PT, "Engenharia de Minas").with(EN, "Mining Engineering");
        final Degree minas = new Degree(minasLS, "EM", null,
                DegreeType.matching(DegreeType::isAdvancedSpecializationDiploma).get(), 30.0, GradeScale.TYPE20,
                "", postGraduate);
        final DegreeCurricularPlan deaem2006 = minas.createDegreeCurricularPlan("DEAEM2006", GradeScale.TYPE20, Authenticate.getUser().getPerson(), academicPeriod);
        deaem2006.setInitialDateYearMonthDay(initialDate);
        deaem2006.setEndDateYearMonthDay(endDate);
        deaem2006.setCurricularStage(CurricularStage.APPROVED);

        final LocalizedString materiaisLS = new LocalizedString(PT, "Engenharia Metalúrgica e de Materiais").with(EN, "Metal and Materials Engineering");
        final Degree materiais = new Degree(materiaisLS, "EMM", null,
                DegreeType.matching(DegreeType::isAdvancedSpecializationDiploma).get(), 30.0, GradeScale.TYPE20,
                "", postGraduate);
        final DegreeCurricularPlan deaemm2006 = materiais.createDegreeCurricularPlan("DEAEMM2006", GradeScale.TYPE20, Authenticate.getUser().getPerson(), academicPeriod);
        deaemm2006.setInitialDateYearMonthDay(initialDate);
        deaemm2006.setEndDateYearMonthDay(endDate);
        deaemm2006.setCurricularStage(CurricularStage.APPROVED);

        final LocalizedString industrialLS = new LocalizedString(PT, "Engenharia e Gestão Industrial").with(EN, "Industrial Engineering and Management");
        final Degree insdustrial = new Degree(industrialLS, "EGI", null,
                DegreeType.matching(DegreeType::isAdvancedSpecializationDiploma).get(), 30.0, GradeScale.TYPE20,
                "", postGraduate);
        final DegreeCurricularPlan deaegi2006 = insdustrial.createDegreeCurricularPlan("DEAEGI2006", GradeScale.TYPE20, Authenticate.getUser().getPerson(), academicPeriod);
        deaegi2006.setInitialDateYearMonthDay(initialDate);
        deaegi2006.setEndDateYearMonthDay(endDate);
        deaegi2006.setCurricularStage(CurricularStage.APPROVED);
    }
}
