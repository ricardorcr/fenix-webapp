package pt.ist.fenix.webapp.task.academic.schedules;

import java.io.ByteArrayOutputStream;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.LessonInstance;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.joda.time.LocalDate;

public class DeleteLessonInstances extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("InstanciasForaPeriodo");
        
        ExecutionSemester executionSemester = ExecutionSemester.readActualExecutionSemester().getNextExecutionPeriod();
        taskLog(executionSemester.getQualifiedName());

        executionSemester.getAssociatedExecutionCoursesSet().stream().flatMap(ec -> ec.getLessons().stream())
                .flatMap(l -> l.getLessonInstancesSet().stream()).filter(li -> !isInLessonPeriod(li))
                .forEach(li -> {
                    final Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("Curso", li.getLesson().getExecutionCourse().getDegreePresentationString());
                    row.setCell("UC",  li.getLesson().getExecutionCourse().getName());
                    row.setCell("Turno",  li.getLesson().getShift().getNome());
                    row.setCell("Instância",  li.getDay().toString());
                 //   li.delete();
                });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("instanciasForaPeriodo.xls", baos.toByteArray());
        taskLog("\nDone");
    }

    private boolean isInLessonPeriod(LessonInstance li) {
        return li.getLesson().getExecutionCourse().getLessonOccupationPeriod().nestedOccupationPeriodsContainsDay(li.getDay());
    }

}