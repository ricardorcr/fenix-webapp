package pt.ist.fenix.webapp.task.gep;

import org.fenixedu.academic.domain.Attends;
import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.EvaluationConfiguration;
import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curriculum.EnrollmentState;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.reports.GepReportFile;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.joda.time.DateTime;

import java.io.ByteArrayOutputStream;
import java.util.Set;

public class EtiReport extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        Spreadsheet spreadsheet = new Spreadsheet("Listagem para ETI");
        Set<EvaluationSeason> seasons = EvaluationConfiguration.getInstance().getEvaluationSeasonSet();
        //TODO Parameters used below, change accordingly!!
        DegreeType degreeType = DegreeType.matching(type -> type.hasExactlyOneCycleType() && type.isFirstCycle()).get();
        ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2023/2024");

        spreadsheet.setHeader("IstID");
        spreadsheet.setHeader("número aluno");
        setDegreeHeaders(spreadsheet, "aluno");
        spreadsheet.setHeader("semestre");
        spreadsheet.setHeader("ano lectivo");
        spreadsheet.setHeader("grupo Disciplina");
        spreadsheet.setHeader("nome Disciplina");
        setDegreeHeaders(spreadsheet, "disciplina");
        spreadsheet.setHeader("creditos");
        spreadsheet.setHeader("estado");
        spreadsheet.setHeader("época");
        spreadsheet.setHeader("nota");
        for (EvaluationSeason season : seasons) {
            spreadsheet.setHeader(season.getName().getContent());
        }
        spreadsheet.setHeader("tipo Aluno");
        spreadsheet.setHeader("número inscricoes anteriores");
        spreadsheet.setHeader("número inscricoes anteriores - via competência");
        spreadsheet.setHeader("código disciplina execução");

        for (final Degree degree : Degree.readNotEmptyDegrees()) {
            if (checkDegreeType(degreeType, degree)) {
                for (final DegreeCurricularPlan degreeCurricularPlan : degree.getDegreeCurricularPlansSet()) {
                    if (checkExecutionYear(executionYear, degreeCurricularPlan)) {
                        for (final CurricularCourse curricularCourse : degreeCurricularPlan.getAllCurricularCourses()) {
                            if (checkExecutionYear(executionYear, curricularCourse)) {

                                for (final CurriculumModule curriculumModule : curricularCourse.getCurriculumModulesSet()) {
                                    if (curriculumModule.isEnrolment()) {
                                        final Enrolment enrolment = (Enrolment) curriculumModule;
                                        if (enrolment.getExecutionYear() == executionYear) {
                                            final ExecutionSemester executionSemester = enrolment.getExecutionPeriod();
                                            if (curricularCourse.isAnual()) {
                                                addEtiRow(spreadsheet, curricularCourse.getDegree(), curricularCourse, enrolment,
                                                        executionSemester, executionSemester, seasons);
                                                if (executionSemester.getSemester() == 1) {
                                                    final ExecutionSemester nextSemester =
                                                            executionSemester.getNextExecutionPeriod();
                                                    addEtiRow(spreadsheet, curricularCourse.getDegree(), curricularCourse,
                                                            enrolment, nextSemester, executionSemester, seasons);
                                                }
                                            } else {
                                                addEtiRow(spreadsheet, curricularCourse.getDegree(), curricularCourse, enrolment,
                                                        executionSemester, executionSemester, seasons);
                                            }

                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output(new DateTime().toString("yyyy_MM_dd_HH_mm") + "_etiGrades_" + degreeType.getName().getContent() + "_" + executionYear.getName() + ".xls", baos.toByteArray());
    }

    private void addEtiRow(final Spreadsheet spreadsheet, final Degree degree, final CurricularCourse curricularCourse,
                           final Enrolment enrolment, final ExecutionSemester executionSemester,
                           final ExecutionSemester executionSemesterForPreviousEnrolmentCount, Set<EvaluationSeason> seasons) {
        final StudentCurricularPlan studentCurricularPlan = enrolment.getStudentCurricularPlan();
        final Registration registration = studentCurricularPlan.getRegistration();
        final Student student = registration.getStudent();

        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell(registration.getPerson().getUsername());
        row.setCell(registration.getNumber());
        setDegreeCells(row, registration.getDegree());
        row.setCell(executionSemester.getSemester().toString());
        row.setCell(executionSemester.getExecutionYear().getYear());
        row.setCell(curricularGroupChain(enrolment));
        row.setCell(curricularCourse.getName());
        setDegreeCells(row, degree);
        row.setCell(enrolment.getEctsCredits().toString().replace('.', ','));
        row.setCell(enrolment.isApproved() ? EnrollmentState.APROVED.getDescription() : enrolment.getEnrollmentState()
                .getDescription());
        row.setCell(enrolment.getEvaluationSeason().getName().getContent());
        row.setCell(enrolment.getGradeValue());

        for (EvaluationSeason season : seasons) {
            row.setCell(
                    enrolment.getFinalEnrolmentEvaluationBySeason(season).map(EnrolmentEvaluation::getGradeValue).orElse(null));
        }

        row.setCell(registration.getRegistrationProtocol().getCode());
        row.setCell(
                String.valueOf(countPreviousEnrolmentsCC(curricularCourse, executionSemesterForPreviousEnrolmentCount, student)));
        row.setCell(countAllPreviousEnrolments(curricularCourse.getCompetenceCourse(), executionSemesterForPreviousEnrolmentCount,
                student));
        Attends attends = null; // enrolment.getAttendsFor(executionSemester);
        for (final Attends a : enrolment.getAttendsSet()) {
            if (a.isFor(executionSemester)) {
                if (attends == null) {
                    attends = a;
                }
            }
        }
        if (attends == null) {
            row.setCell("");
        } else {
            final ExecutionCourse executionCourse = attends.getExecutionCourse();
            row.setCell(GepReportFile.getExecutionCourseCode(executionCourse));
        }
    }

    private String curricularGroupChain(final Enrolment enrolment) {
        final StringBuilder builder = new StringBuilder();
        final CurriculumGroup curriculumGroup = enrolment.getCurriculumGroup();
        curricularGroupChain(builder, curriculumGroup);
        return builder.toString();
    }

    private void curricularGroupChain(final StringBuilder builder, final CurriculumGroup curriculumGroup) {
        final CurriculumGroup parent = curriculumGroup.getCurriculumGroup();
        if (parent != null && !parent.isRoot()) {
            curricularGroupChain(builder, parent);
        }
        if (builder.length() > 0) {
            builder.append(" > ");
        }
        builder.append(curriculumGroup.getPresentationName().getContent());
    }

    private String countAllPreviousEnrolments(final CompetenceCourse competenceCourse, final ExecutionSemester executionPeriod,
                                              final Student student) {
        int count = 0;
        if (competenceCourse == null) {
            return Integer.toString(count);
        }
        count = competenceCourse.getAssociatedCurricularCoursesSet().stream()
                .mapToInt(cc -> countPreviousEnrolmentsCC(cc, executionPeriod, student)).sum();
        return Integer.toString(count);
    }

    private int countPreviousEnrolmentsCC(final CurricularCourse curricularCourse, final ExecutionSemester executionPeriod,
                                          final Student student) {
        return (int) curricularCourse.getCurriculumModulesSet().stream()
                .filter(CurriculumModule::isEnrolment)
                .map(curriculumModule -> (Enrolment) curriculumModule)
                .filter(enrolment -> executionPeriod.compareTo(enrolment.getExecutionPeriod()) > 0)
                .filter(enrolment -> enrolment.getStudentCurricularPlan().getRegistration().getStudent() == student)
                .count();
    }

    protected void setDegreeHeaders(final Spreadsheet spreadsheet, final String suffix) {
        spreadsheet.setHeader("tipo curso " + suffix);
        spreadsheet.setHeader("nome curso " + suffix);
        spreadsheet.setHeader("sigla curso " + suffix);
    }

    protected void setDegreeCells(final Spreadsheet.Row row, final Degree degree) {
        row.setCell(degree.getDegreeType().getName().getContent());
        row.setCell(degree.getNameI18N().getContent());
        row.setCell(degree.getSigla());
    }

    protected static boolean checkDegreeType(final DegreeType degreeType, final Degree degree) {
        return degreeType == null || degree.getDegreeType() == degreeType;
    }

    protected static boolean checkExecutionYear(final ExecutionYear executionYear, final DegreeCurricularPlan degreeCurricularPlan) {
        return executionYear == null || degreeCurricularPlan.hasExecutionDegreeFor(executionYear);
    }

    protected static boolean checkExecutionYear(ExecutionYear executionYear, final CurricularCourse curricularCourse) {
        return executionYear == null || curricularCourse.isActive(executionYear);
    }
}
