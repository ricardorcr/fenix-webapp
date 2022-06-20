package pt.ist.fenix.webapp.task;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class TestAutoCommitTask2 extends CustomTask {
    @Override
    public void runTask() throws Exception {
        taskLog("Testing auto commit of custom tasks into git repo.");
    }
}