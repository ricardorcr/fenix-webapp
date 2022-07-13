package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionsQueue;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.queueing.domain.AttendanceLocation;
import org.fenixedu.queueing.domain.QueueingSystem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.Collectors;

public class ExampleQueueConfigSpreadsheet extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(admissionProcess -> admissionProcess.getTitle().getContent().indexOf("2023") >= 0)
                .filter(admissionProcess -> Utils.isDegreeType(admissionProcess))
                .forEach(admissionProcess -> {
                    final Spreadsheet spreadsheet = queueConfigSpreadsheet(admissionProcess);
                    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    try {
                        spreadsheet.exportToXLSSheet(stream);
                        output(admissionProcess.getTitle().getContent() + ".xlsx", stream.toByteArray());
                    } catch (final IOException e) {
                        throw new Error(e);
                    }
                });
    }

    public Spreadsheet queueConfigSpreadsheet(final AdmissionProcess admissionProcess) {
        final Spreadsheet spreadsheet = new Spreadsheet("OutcomeQueue");
        final Spreadsheet spreadsheetBefore = new Spreadsheet("BeforeOutcomeQueue");
        admissionProcess.getAdmissionProcessTargetSet().forEach(target -> {
            final String name = target.getName().getContent();
            append(spreadsheet, name, target.getAfterOutcomeQueue());
            append(spreadsheetBefore, name, target.getBeforeOutcomeQueue());
        });

        final Spreadsheet queueSheet = spreadsheet.addSpreadsheet("Queues");
        QueueingSystem.getInstance().getAttendanceQueueSet().forEach(attendanceQueue -> {
            queueSheet.addRow()
                    .setCell("QueueID", attendanceQueue.getExternalId())
                    .setCell("Description", attendanceQueue.getDescription().getContent())
                    .setCell("Locations", attendanceQueue.getAttendanceLocationSet().stream()
                            .map(attendanceLocation -> attendanceLocation.getDescription())
                            .collect(Collectors.joining("; ")));
        });
        return spreadsheet;
    }

    private void append(final Spreadsheet spreadsheet, final String name, final AdmissionsQueue queue) {
        final AttendanceLocation location = queue == null ? null : attendanceLocation(queue);
        spreadsheet.addRow().setCell("Target", name)
                .setCell("Start", queue == null ? null : queue.startSchedulePeriod.toString("yyyy-MM-dd HH:mm"))
                .setCell("End", queue == null ? null : queue.endSchedulePeriod.toString("yyyy-MM-dd HH:mm"))
                .setCell("Location", location == null ? null : location.getDescription());
    }

    public AttendanceLocation attendanceLocation(final AdmissionsQueue queue) {
        return queue.queue.getAttendanceLocationSet().stream()
                .findAny().orElse(null);
    }

}