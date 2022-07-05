package pt.ist.fenix.webapp.task;

import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.connect.domain.ConnectSystem;

public class DebugIdentities extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        ConnectSystem.getInstance().getIdentitySet().stream()
                .filter(identity -> identity.getPersonalInformation() == null)
//                .map(identity -> identity.getPersonalInformation())
//                .filter(personalInformation -> personalInformation.getIdentificationDocument() == null)
//                .forEach(personalInformation -> {
                  .forEach(identity -> {
                    //taskLog("No ID Document for identity %s%n", personalInformation.getIdentity().getExternalId());
                      taskLog("No personal information for: %s%n", identity.getExternalId());
                });
    }

}