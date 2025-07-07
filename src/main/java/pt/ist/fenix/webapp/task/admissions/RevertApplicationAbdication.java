package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.admissions.domain.Application;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.smartFlow.domain.Flow;
import org.fenixedu.smartFlow.domain.FlowState;
import pt.ist.fenixframework.FenixFramework;

public class RevertApplicationAbdication extends CustomTask {

    @Override
    public void runTask() throws Exception {
        fix("852890310687683");
    }

    private void fix(final String id) {
        final Application application = FenixFramework.getDomainObject(id);
        application.setAbdicated(false);
        final Flow flow = JsonUtils.toDomainObject(application.getDataObject(), "flow");
        flow.undoLastAction("Revert Abdication", true);
        flow.setState(FlowState.RUNNING);
    }
}
