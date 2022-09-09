package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.service.CiistAdminUserAPI;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class TestLdapInfo extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Application application = FenixFramework.getDomainObject("571415333969716");
        final CiistAdminUserAPI api = new CiistAdminUserAPI();
        final JsonObject jsonObject = api.userInfo(application.getAccount().getUser().getUsername());
        taskLog(jsonObject.toString());
    }
}