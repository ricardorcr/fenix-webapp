package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class FixDoubleUserIdentity extends CustomTask {

    @Override
    public void runTask() throws Exception {
//        final Identity identity = FenixFramework.getDomainObject("289970422022315");
//        identity.getPersonSet().stream()
//            .filter(p -> p.getExternalId().equals("1128206304286717")) //the person that does not have the registration
//            .forEach(p -> {                
//                identity.getPersonSet().remove(p);
//                identity.getNotPersonSet().add(p);
//                identity.setLastLdapExport(null);
//            });        
    }
}
