package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.registrationStates.RegisteredState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

import java.util.Locale;

public class SetIngressionTypeInRegistrations extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final IngressionType ingressionType = IngressionType.findIngressionTypeByCode("EINT").get(); //Internacionais
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(process -> {
                    final String title = process.getTitle().getContent(Locale.forLanguageTag("pt-PT"));
                    return title.contains("Internacionais");
                })
                .flatMap(process -> process.getAdmissionProcessTargetSet().stream())
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(application -> application.getDataObject().has("registration"))
                .forEach(application -> setIngressionType(application, ingressionType, false));

        final IngressionType degreeChangeIngressionType = IngressionType.findIngressionTypeByCode("MPIC").get(); //Mudança Par/Curso -- ou MC??
        AdmissionProcess admissionProcess = FenixFramework.getDomainObject("289957537120269"); //mudança par/curso
        admissionProcess.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(application -> application.getDataObject().has("registration"))
                .forEach(application -> setIngressionType(application, degreeChangeIngressionType, true));
    }

    private void setIngressionType(final Application application, final IngressionType ingressionType, boolean changeState) {
        final String registrationID = application.getDataObject().get("registration").getAsString();
        Registration registration = FenixFramework.getDomainObject(registrationID);
        if (registration.getIngressionType() != null && registration.getIngressionType() == ingressionType) {
//            taskLog("Já tinha %s\t%s%n", registration.getExternalId(), registration.getNumber());
        } else {
            taskLog("Vou mudar para: %s\t%s\t%s%n", application.getAdmissionProcessTarget().getAdmissionProcess().getTitle().getContent(),
                    registration.getNumber(), registration.getDegree().getSigla());
            registration.setIngressionType(ingressionType);
        }
//        if (changeState) {
//           final long count =
//                    registration.getStudent().getRegistrationStream()
//                    .filter(r -> r != registration)
//                    .filter(r -> !r.getDegree().isEmpty())
//                    .filter(r -> r.getActiveState().getStateType().equals(RegistrationStateType.REGISTERED))
//                    .count();
//            registration.getStudent().getRegistrationStream()
//                    .filter(r -> r != registration)
//                    .filter(r -> !r.getDegree().isEmpty())
//                    .filter(r -> r.getActiveState().getStateType().equals(RegistrationStateType.REGISTERED))
//            .forEach(r -> {
//                taskLog("Posta em Abandono Interno (for real): %s\t%s%n", r.getDegree().getSigla(), r.getPerson().getUsername());
//                RegisteredState.createRegistrationState(r, r.getPerson(), new DateTime(), RegistrationStateType.INTERNAL_ABANDON);
//            });
//            if (count > 0) {
//                taskLog(" --> %s%n", count);
//            }
//        }
    }
}
