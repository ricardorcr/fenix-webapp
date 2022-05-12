package pt.ist.fenix.webapp.task.connect;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.TeacherAuthorization;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.person.IDDocumentType;
import org.fenixedu.academic.dto.person.PersonBean;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.UserAccountInfo;
import org.fenixedu.admissions.ist.util.IdentificationDocumentConverter;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.connect.domain.identification.Gender;
import org.fenixedu.connect.domain.identification.IdentificationDocument;
import org.fenixedu.connect.domain.identification.PersonalInformation;
import org.fenixedu.connect.domain.identification.TaxInformation;
import org.fenixedu.connect.service.IdentityValidationService;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import pt.ist.fenixframework.FenixFramework;

import java.util.Objects;

public class SetupNewAccountAsInternalUser extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Account account = FenixFramework.getDomainObject("TODO");
        if (account.getIdentity() == null) {
            IdentityValidationService.verifyIdentity(account);
        }
        final Person person = personFor(account);
        final Teacher teacher = person.getTeacher() == null
                ? new Teacher(person) : person.getTeacher();
        TeacherAuthorization.createOrUpdate(teacher,
                Bennu.getInstance().getDepartmentsSet().stream().findAny().get(),
                ExecutionSemester.readActualExecutionSemester(),
                Bennu.getInstance().getTeacherCategorySet().stream().findAny().get(),
                Boolean.FALSE,
                1d);
    }

    private Person personFor(final Account account) {
        final Identity identity = account.getIdentity();
        if (identity != null) {
            final User user = identity.getUser();
            if (user != null) {
                final Person person = user.getPerson();
                if (person != null) {
                    return person;
                }
            }
            final Person person = identity.getAccountSet().stream()
                    .map(Account::getUser)
                    .filter(Objects::nonNull)
                    .map(User::getPerson)
                    .filter(Objects::nonNull)
                    .findAny().orElse(null);
            if (person != null) {
                return person;
            }
        }
        final User user = account.getUser();
        if (user != null) {
            final Person person = user.getPerson();
            if (person != null) {
                return person;
            }
        }
        return account.getApplicationSet().stream()
                .map(Application::getEvent)
                .filter(Objects::nonNull)
                .map(Event::getPerson)
                .filter(Objects::nonNull)
                .findAny().orElseGet(() -> createPerson(account));
    }

    private Person createPerson(final Account account) {
        final Identity identity = account.getIdentity();
        final PersonalInformation personalInformation = identity == null ? account.getPersonalInformation() : identity.getPersonalInformation();
        final IdentificationDocument identificationDocument = personalInformation.getIdentificationDocument();
        final IDDocumentType idDocumentType = IdentificationDocumentConverter.toIdDocumentType(identificationDocument);
        final TaxInformation taxInformation = personalInformation == null ? null : personalInformation.getTaxInformation();

        Person person = Person.readByDocumentIdNumberAndIdDocumentType(identificationDocument.getDocumentNumber(), idDocumentType);
        if (person == null) {
            final PersonBean personBean = new PersonBean(personalInformation.getFullName(),
                    identificationDocument.getDocumentNumber(),
                    idDocumentType,
                    toYearMonthDay(personalInformation.getDateOfBirth()));
            personBean.setGivenNames(personalInformation.getGivenName());
            personBean.setFamilyNames(personalInformation.getFamilyName());
            personBean.setGender(personalInformation.getGender() != null
                    && personalInformation.getGender() == Gender.MALE
                    ? org.fenixedu.academic.domain.person.Gender.MALE
                    : org.fenixedu.academic.domain.person.Gender.FEMALE);
            final Country country = Country.readByTwoLetterCode(personalInformation.getNationalityCountryCode());
            personBean.setNationality(country);
            person = new Person(personBean, false);
        } else if (person.getUser() != null) {
            throw new Error("error.person.already.connected.to.user");
        }
        if (taxInformation != null) {
            person.setSocialSecurityNumber(taxInformation.getTin());
        }
        // Sincronize with update method
        if(personalInformation != null) {
            UserAccountInfo.updatePersonFromPersonalInfo(personalInformation, person);
            UserAccountInfo.updatePersonFromEmergencyContact(personalInformation.getEmergencyContact(), person);
        }
        return person;
    }

    private YearMonthDay toYearMonthDay(final LocalDate localDate) {
        return new YearMonthDay(localDate.getYear(), localDate.getMonthOfYear(), localDate.getDayOfMonth());
    }

}
