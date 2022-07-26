package pt.ist.fenix.webapp.task.academic.enrolment;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CheckSpecialSeasonEnrolmentLimits extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("SpecialSeason");
        ExecutionYear.readCurrentExecutionYear().getExecutionPeriodsSet().stream()
                .flatMap(semester -> semester.getEnrolmentsSet().stream())
                .filter(enrolment -> enrolment.hasSpecialSeason())
                .collect(Collectors.toMap(enrolment -> enrolment.getRegistration().getPerson().getUser(),
                        enrolment -> Stream.of(enrolment),
                        (s1, s2) -> Stream.concat(s1, s2)))
                .forEach((user, stream) -> {
                    double[] d = new double[] { 0d, 0d, 0d };
                    stream.forEach(enrolment -> {
                        final double ects = enrolment.getEctsCredits().doubleValue();
                        final int semester = enrolment.getExecutionPeriod().getSemester().intValue();
                        d[semester - 1] += ects;
                        d[2] += ects;
                    });
                    spreadsheet.addRow()
                            .setCell("User", user.getUsername())
                            .setCell("Semester 1", d[0])
                            .setCell("Semester 2", d[1])
                            .setCell("Total", d[2]);
                });
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("specialSeason.xlsx", stream.toByteArray());
    }

}