package pt.ist.fenix.webapp.task;

import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.fenixedu.connect.domain.ConnectSystem;

public class DebugIdentities extends WriteCustomTask {

    @Override
    public void runTask() throws Exception {
        ConnectSystem.getInstance().getIdentitySet().stream()
                .filter(identity -> identity.getPersonalInformation() == null)
//                .map(identity -> identity.getPersonalInformation())
//                .filter(personalInformation -> personalInformation.getIdentificationDocument() == null)
//                .forEach(personalInformation -> {
                  .forEach(identity -> {
                      identity.setUser(null);
                      identity.getAccountSet().stream().forEach(account -> {
                          account.setUser(null);
                          account.delete();
                      });
//                      identity.delete();
                });
    }

}