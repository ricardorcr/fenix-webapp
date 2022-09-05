package org.fenixedu.admissions.ist.task;

import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.connect.domain.ConfirmationCode;
import org.fenixedu.connect.domain.ConnectSystem;
import org.joda.time.DateTime;

@Task(englishTitle = "Oauth update date of token")
public class OauthUpdateDateOfToken extends CustomTask {
    @Override
    public void runTask() throws Exception {
        for(ConfirmationCode confirmationCode: ConnectSystem.getInstance().getConfirmationCodeSet()){
            if(confirmationCode.getAccount().isOauthAccount()) {
                confirmationCode.setTokenValidUntil(new DateTime().plusYears(1));
            }
        }
    }
}