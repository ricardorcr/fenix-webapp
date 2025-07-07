package pt.ist.fenix.webapp.task.tutor;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTimeFieldType;

import java.util.HashMap;
import java.util.Map;

public class FixStartExecutionYearTutorship extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Integer count[] = new Integer[] {0};
        Map<String, Integer[]> years = new HashMap<>();
        Bennu.getInstance().getTutorshipsSet().stream()
                .filter(tutorship -> tutorship.getStartExecutionYearOverride() == null)
                .filter(tutorship -> tutorship.getStartDate() != null)
                .forEach(tutorship -> {
                    final ExecutionYear firstYear = tutorship.getCoveredExecutionYears().get(0);
                    int tutorshipMonth = tutorship.getStartDate().get(DateTimeFieldType.monthOfYear());
                    int tutorshipYear = tutorship.getStartDate().get(DateTimeFieldType.year());
                    int firstYearMonth = firstYear.getBeginLocalDate().getMonthOfYear();
                    if (firstYearMonth > tutorshipMonth) {
                        if (firstYearMonth - tutorshipMonth == 1) {
                            count[0] = count[0] + 1;
                            years.computeIfAbsent(firstYear.getName(), (k) -> new Integer[]{0})[0]++;
                            taskLog("%s %s - %s %s %s%n", firstYearMonth, firstYear.getName(), tutorshipMonth, tutorshipYear, tutorship.getExternalId());
                            tutorship.setStartExecutionYearOverride(firstYear.getNextExecutionYear());
                        }
                    }
                });

        years.forEach((k,v) -> taskLog("%s\t%s%n", k, v[0]));
        taskLog("Quantos são? %s%n", count[0]);
    }
}
