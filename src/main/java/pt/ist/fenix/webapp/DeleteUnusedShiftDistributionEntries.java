package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.candidacy.degree.ShiftDistributionEntry;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class DeleteUnusedShiftDistributionEntries extends CustomTask {

    @Override
    public void runTask() throws Exception {
        ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();

        for (ShiftDistributionEntry shiftDistributionEntry : currentExecutionYear.getShiftDistribution()
                .getShiftDistributionEntriesSet()) {
            if (!shiftDistributionEntry.alreadyDistributed()) {
                //taskLog("Delete entry with name " + shiftDistributionEntry.getShift().getNome());
                shiftDistributionEntry.delete();
            } else {
                taskLog("Já está distribuido %s%n", shiftDistributionEntry.getShift().getNome());
            }
        }
    }
}