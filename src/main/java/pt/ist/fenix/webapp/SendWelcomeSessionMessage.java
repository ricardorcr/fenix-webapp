package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.messaging.core.domain.Message;
import org.fenixedu.messaging.core.domain.MessagingSystem;
import org.fenixedu.messaging.core.domain.Sender;

import pt.ist.fenixframework.FenixFramework;

public class SendWelcomeSessionMessage extends CustomTask {

    final String templateKey2ndCycle = "admissions.externalCandidate.welcome.session.2ndCycle";
    final String templateKey1stCycle = "admissions.externalCandidate.welcome.session.1stCycle";
    final String templateKey = "admissions.externalCandidate.welcome.session";

    @Override
    public void runTask() throws Exception {

//        send("luis.cruz@tecnico.pt", "Luis Cruz");
//        send("ricardo.rodrigues@tecnico.ulisboa.pt", "Ricardo Rodrigues");

        final int[] c = new int[] { 0 };
//        AdmissionsSystem.getInstance().getCandidateSet().stream()
//                .filter(ExternalCandidate.class::isInstance)
//                .map(Candidate::getIdentity)
//                .filter(identity -> identity != null)
//                .filter(identity -> identity.getCandidateSet().stream().anyMatch(candidate -> !candidate.getApplicationSet().isEmpty()))
//                .filter(identity -> identity.getPersonSet().stream().anyMatch(person -> person.getStudent() != null))
//                .filter(this::is2ndCycle)
//                .peek(identity -> c[0]++)
//                .forEach(identity -> {
//                    final String email = identity.getCandidateSet().stream()
//                            .filter(ExternalCandidate.class::isInstance)
//                            .map(candidate -> candidate.getEmail())
//                            .findAny()
//                            .orElseThrow(() -> new Error());
//                    final String name = identity.getCandidateSet().stream()
//                            .filter(ExternalCandidate.class::isInstance)
//                            .map(candidate -> candidate.getDisplayName())
//                            .findAny()
//                            .orElseThrow(() -> new Error());
//                    taskLog("%s %s%n", email, name);
//                    send(email, name);
//                });
//
//        taskLog("Sent to %s students%n", c[0]);
    }

//    private boolean is2ndCycle(Identity identity) {
//        return identity.getUser().getPerson().getStudent().getRegistrationsSet().stream()
//            .map(reg -> reg.getDegree())
//            .anyMatch(degree -> degree.isSecondCycle());
//    }

    
    private void send(final String email, final String name) {
        final Sender sender = FenixFramework.getDomainObject("1696378937945242");
        final Message message = Message.from(sender)
                .singleTos(email)
                .template(templateKey)
                .parameter("name", name)
                .and()
                .wrapped()
                .send();
        
        MessagingSystem.dispatch(message);
    }

}