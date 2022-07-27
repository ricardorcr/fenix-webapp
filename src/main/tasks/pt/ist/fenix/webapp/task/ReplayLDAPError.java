package pt.ist.fenix.webapp.task;

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
/*
        final String password = JsonUtils.parse(body).get("password").getAsString();
        if (loggedAccount.has2FALogin(twoFactorToken) || !loggedAccount.has2FAMethod()) {
            pluggablePassword.setPassword(loggedAccount, password);
            setPassword(loggedAccount);
            return ResponseEntity.ok().build();
        }
        // Logout for security purposes.
        // If the logged account does not have an active 2fa token, this is probably a request made outside the application.
        Authenticate.logout(httpRequest, httpResponse);
        throw new ConnectError(HttpStatus.FORBIDDEN, "error.account.has.no.2fa.login");
*/
    }
}