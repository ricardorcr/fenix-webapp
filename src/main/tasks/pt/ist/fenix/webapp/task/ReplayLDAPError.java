package pt.ist.fenix.webapp.task;

import com.google.gson.JsonObject;
import org.fenixedu.admissions.ist.domain.UserAccountInfo;
import org.fenixedu.admissions.ist.service.CiistAdminUserAPI;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.pluggable.password.PluggablePassword;
import org.fenixedu.connect.util.ConnectError;
import org.springframework.http.HttpStatus;
import pt.ist.fenixframework.FenixFramework;

public class ReplayLDAPError extends ReadCustomTask {
    @Override
    public void runTask() throws Exception {
        final Account loggedAccount = FenixFramework.getDomainObject("853087879175646");
        final PluggablePassword pluggablePassword = PluggablePassword.getInstance();

        if (pluggablePassword == null) {
            throw new ConnectError(HttpStatus.PRECONDITION_FAILED, "pluggable.password.not.registered");
        }

        if (!pluggablePassword.canSetPassword(loggedAccount)) {
            throw new ConnectError(HttpStatus.PRECONDITION_FAILED, "account.cannot.set.password");
        }

        final String password = "Avdflksjrgflksjfg%&//%(%(/&%!";
        //if (loggedAccount.has2FALogin(twoFactorToken) || !loggedAccount.has2FAMethod()) {
            setPassword(loggedAccount, password);
        //}
    }

    public void setPassword(final Account account, final String password) {
        //if (canSetPassword(account)) {
            final CiistAdminUserAPI api = new CiistAdminUserAPI();
            final JsonObject userConnect = api.userInfo(UserAccountInfo.usernameFor(account));
            if (userConnect == null) {
                taskLog("1");
                api.createUser(UserAccountInfo.usernameFor(account));
            }
            taskLog("2");
            api.createOrEditPassword(UserAccountInfo.usernameFor(account), password);
        //}
    }

}