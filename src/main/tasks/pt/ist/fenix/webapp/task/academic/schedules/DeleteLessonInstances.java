package pt.ist.fenix.webapp.task.academic.schedules;

import java.io.ByteArrayOutputStream;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.joda.time.LocalDate;

public class DeleteLessonInstances extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("InstanciasDia23");
        
        
        LocalDate date = new LocalDate(2022, 12, 23);
        ExecutionSemester executionSemester = ExecutionSemester.readActualExecutionSemester().getNextExecutionPeriod();
        taskLog(executionSemester.getQualifiedName());

        executionSemester.getAssociatedExecutionCoursesSet().stream().flatMap(ec -> ec.getLessons().stream())
                .flatMap(l -> l.getLessonInstancesSet().stream()).filter(li -> li.getDay().toLocalDate().equals(date))
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
        output("instanciasDia23.xls", baos.toByteArray());
        taskLog("\nDone");
    }

}