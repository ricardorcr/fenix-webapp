package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.EmailAddress;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.admissions.ist.service.CiistAdminUserAPI;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixframework.FenixFramework;

public class CheckAndFixAlreadyHasInstitutionalEmail extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
//        ExecutionSemester.readActualExecutionSemester().getEnrolmentsSet().stream()
//                .map(CurriculumModule::getRegistration)
//                .distinct()
//                .map(Registration::getPerson)
//                .filter(p -> p.getInstitutionalEmailAddress() == null)
//                .filter(p -> getInstitutionalEmail(p) != null)
//                .forEach(this::fix);

//        FenixFramework.atomic(() -> {
//                    User.findByUsername("ist193881").getPerson().setInstitutionalEmailAddressValue("margaridamoreira121@tecnico.ulisboa.pt");
//                });

        Bennu.getInstance().getPartysSet().stream()
                .filter(Person.class::isInstance)
                .map(Person.class::cast)
                .filter(person -> person.getInstitutionalEmailAddress() == null)
                .filter(person -> person.getUser() != null)
                .filter(person -> getInstitutionalEmail(person) != null)
                .forEach(this::fix);
    }

    private void fix(final Person person) {
//        FenixFramework.atomic(() -> {
            final CiistAdminUserAPI api = new CiistAdminUserAPI();
            JsonObject json = api.userInfo(person.getUsername());
        if (json != null) {
            final String email = api.getEmail(json);
            final boolean isMailActive = json.get("isMailActive").getAsBoolean();
            final EmailAddress institutionalEmail = getInstitutionalEmail(person);
            final String mailValue = institutionalEmail.getPresentationValue();
            final String fenixMail = mailValue;
            if (isMailActive && email.equals(fenixMail)) {
//                institutionalEmail.setValue("toDelete" + mailValue);
//                person.setInstitutionalEmailAddressValue(mailValue);
//                institutionalEmail.delete();
                taskLog("%s\t%s\t%s\t%s\t%s\t%s%n", person.getUsername(), person.getStudent() != null, fenixMail, email, isMailActive, email.equals(fenixMail));
            }
        }
//        });
    }

    private EmailAddress getInstitutionalEmail(Person person) {
        return person.getEmailAddressStream()
                .filter(email -> email.getPresentationValue().endsWith("@tecnico.ulisboa.pt"))
                .findAny().orElse(null);
    }
}
