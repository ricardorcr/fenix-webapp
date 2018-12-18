package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.OptionalEnrolment;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CycleCourseGroup;
import org.fenixedu.academic.domain.degreeStructure.OptionalCurricularCourse;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;
import java.util.Collection;

public class CheckOptionalEnrolments extends ReadCustomTask {

    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Opcionais de um ciclo inferior");
        ExecutionSemester.readActualExecutionSemester().getEnrolmentsSet().stream()
                .filter(enrolment -> enrolment.isOptional())
                .map(OptionalEnrolment.class::cast)
                .filter(optionalEnrolment -> {
                    final OptionalCurricularCourse optionalCurricularCourse = optionalEnrolment.getOptionalCurricularCourse();
                    final CurricularCourse curricularCourse = optionalEnrolment.getCurricularCourse();

                    boolean isCycleEqualOrGreater = false;
                    final Collection<CycleCourseGroup> parentCycleCourseGroups = curricularCourse.getParentCycleCourseGroups();
                    for (CycleCourseGroup cycleCourseGroup : optionalCurricularCourse.getParentCycleCourseGroups()) {
                        if (hasSameOrHigherCycle(cycleCourseGroup, parentCycleCourseGroups)) {
                            isCycleEqualOrGreater = true;
                            break;
                        }
                    }
                    final DegreeType degreeType = curricularCourse.getDegreeType();
                    if (!isCycleEqualOrGreater && !degreeType.getUnstructured() && !degreeType.getMinor()) {
                        return true; //inscrito numa opcional de um ciclo inferior
                    } else {
                        return false;
                    }
                })
                .forEach(optionalEnrolment -> report(optionalEnrolment, spreadsheet));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("inscricoes_opcionais_ciclo_errado.xls", baos.toByteArray());
    }

    private boolean hasSameOrHigherCycle(final CycleCourseGroup cycleCourseGroup, final Collection<CycleCourseGroup> parentCycleCourseGroups) {
        return parentCycleCourseGroups.stream()
                .anyMatch(parentCycleCourseGroup -> cycleCourseGroup.getCycleType().isBeforeOrEquals(parentCycleCourseGroup.getCycleType()));
    }

    private void report(final OptionalEnrolment optionalEnrolment, final Spreadsheet spreadsheet) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        final Person student = optionalEnrolment.getRegistration().getPerson();
        row.setCell("IstID", student.getUsername());
        row.setCell("Nome", student.getName());
        row.setCell("Disciplina Currículo", optionalEnrolment.getName().getContent());
        row.setCell("Disciplina Escolhida", optionalEnrolment.getCurricularCourse().getName());
        row.setCell("Curso Aluno", optionalEnrolment.getStudentCurricularPlan().getDegreeCurricularPlan().getName());
    }
}
