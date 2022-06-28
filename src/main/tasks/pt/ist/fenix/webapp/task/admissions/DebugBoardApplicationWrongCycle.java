package pt.ist.fenix.webapp.task.admissions;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.bennu.core.util.TransactionalThread;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixframework.FenixFramework;

public class DebugBoardApplicationWrongCycle extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        try {
            TransactionalThread.runTx(false, () -> {
                taskLog("Test 1... as is.");
                final Application application = FenixFramework.getDomainObject("571415333968181");
                application.board();
                throw new Error("Abort TX");
            });
        } catch (final Throwable t) {
            taskLog("Fail: %s%n", t.getMessage());
            if (t.getCause() != null) {
                taskLog("   -> %s%n", t.getCause().getMessage());
            }
        }

        try {
            TransactionalThread.runTx(false, () -> {
                taskLog("Test 2... hack cycle type.");
                final Application application = FenixFramework.getDomainObject("571415333968181");
                final AdmissionProcessTarget target = application.getAdmissionProcessTarget();
                final JsonObject outcome = target.getOutcomeConfigJson();
                outcome.addProperty("cycleType", CycleType.SECOND_CYCLE.name());
                target.setOutcomeConfig(outcome.toString());
                application.board();
                throw new Error("Abort TX");
            });
        } catch (final Throwable t) {
            taskLog("Fail: %s%n", t.getMessage());
            if (t.getCause() != null) {
                taskLog("   -> %s%n", t.getCause().getMessage());
            }
        }

        throw new Error("Abort TX");
    }
}