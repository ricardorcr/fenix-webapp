package pt.ist.fenix.webapp.task.connect;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.connect.domain.identification.PersonalInformation;
import pt.ist.fenixframework.FenixFramework;

public class HackAccounts extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final User user1 = User.findByUsername("ist30047");
        final User user2 = User.findByUsername("ist419033");

//        final Account account1 = FenixFramework.getDomainObject("1697512810064735");
//        account1.getIdentity().setUser(User.findByUsername("ist419033"));

        accept(user2, user1);
    }

    private void accept(User newUser, User oldUser) {
        final Student studentFromNewUser = studentFor(newUser);
        final Student studentFromOldUser = studentFor(oldUser);
        if (studentFromNewUser != null && studentFromOldUser != null) {
            studentFromOldUser.getRegistrationsSet().forEach(registration -> registration.setStudent(studentFromNewUser));
            studentFromOldUser.getPerson().getEventsSet().stream().forEach(event -> event.setParty(studentFromNewUser.getPerson()));
        } else if (studentFromOldUser != null) {
            studentFromOldUser.setPerson(newUser.getPerson());
        }
    }

    private Student studentFor(final User user) {
        final Person person = user == null ? null : user.getPerson();
        return person == null ? null : person.getStudent();
    }

}