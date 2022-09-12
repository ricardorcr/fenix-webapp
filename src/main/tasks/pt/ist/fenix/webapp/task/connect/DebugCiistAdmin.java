package pt.ist.fenix.webapp.task.connect;

import com.google.gson.JsonObject;
import org.fenixedu.admissions.ist.service.CiistAdminUserAPI;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;

public class DebugCiistAdmin extends ReadCustomTask {
    @Override
    public void runTask() throws Exception {
        final CiistAdminUserAPI api = new CiistAdminUserAPI();
        final JsonObject json = api.userInfo("ist1105022");
        taskLog("json= %s%n", json);
        //return api.canSetEmail();

    }
}