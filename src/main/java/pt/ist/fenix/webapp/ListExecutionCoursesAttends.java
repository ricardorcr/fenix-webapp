package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;

public class ListExecutionCoursesAttends extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Spreadsheet spreadsheet = new Spreadsheet("Alunos a frequentar");
        ExecutionDegree executionDegree = FenixFramework.getDomainObject("563808946880636"); //MPSR 2021
        final Set<ExecutionCourse> executionCourseSet = new HashSet<>();
        final ExecutionSemester actualExecutionSemester = ExecutionSemester.readActualExecutionSemester();
        executionCourseSet.addAll(executionDegree.getDegreeCurricularPlan().getExecutionCoursesByExecutionPeriod(actualExecutionSemester));
        executionCourseSet.addAll(executionDegree.getDegreeCurricularPlan().getExecutionCoursesByExecutionPeriod(actualExecutionSemester.getNextExecutionPeriod()));

        executionCourseSet.stream()
                .filter(ec -> !ec.getAttendsSet().isEmpty())
                .forEach(ec -> report(spreadsheet, ec));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("alunos_frequentar.xlsx", baos.toByteArray());
    }

    private void report(final Spreadsheet spreadsheet, final ExecutionCourse executionCourse) {
        executionCourse.getAttendsSet().stream()
                .forEach(att -> {
                    final Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("Aluno", att.getRegistration().getNumber());
                    row.setCell("Disciplina", executionCourse.getName());
                    row.setCell("Semestre", executionCourse.getExecutionPeriod().getQualifiedName());
                    row.setCell("Turnos", executionCourse.getAssociatedShifts().size());
                });
    }
}
