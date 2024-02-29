package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.EventTemplate;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreateEventsFromTemplate extends ReadCustomTask {

    List<String> createForIDs = null;

    @Override
    public void runTask() throws Exception {
        createForIDs = Arrays.asList("ist128442","ist168402","ist189673","ist186335","ist198566"); //21/22
        ExecutionYear.readCurrentExecutionYear().getPreviousExecutionYear().getPreviousExecutionYear().getRegistrationDataByExecutionYearSet()
                .forEach(this::process);
        createForIDs = Arrays.asList("ist1102617","ist187841","ist198566"); //22/23
        ExecutionYear.readCurrentExecutionYear().getPreviousExecutionYear().getRegistrationDataByExecutionYearSet()
                .forEach(this::process);
    }

    private void process(final RegistrationDataByExecutionYear dataByExecutionYear) {
        if (createForIDs.contains(dataByExecutionYear.getRegistration().getPerson().getUsername())) {
            try {
                FenixFramework.atomic(() -> {
                    final EventTemplate eventTemplate = EventTemplate.templateFor(dataByExecutionYear);
                    if (eventTemplate != null) {
                        eventTemplate.createEventsFor(dataByExecutionYear);
                    }
                });
            } catch (final Throwable t) {
                throw new Error(t);
            }
        }
    }
}