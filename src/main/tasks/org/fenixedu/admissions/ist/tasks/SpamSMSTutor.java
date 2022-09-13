package org.fenixedu.admissions.ist.tasks;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.person.Gender;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.util.StringFormatter;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.fenixedu.messaging.core.domain.Message;
import org.fenixedu.messaging.smsdispatch.SMSMessage;
import pt.ist.fenixedu.tutorship.domain.Tutorship;
import pt.ist.fenixframework.FenixFramework;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SpamSMSTutor extends WriteCustomTask {

    private static final String SPAM_FILENAME = "/afs/ist.utl.pt/ciist/fenix/fenix036/tuitionSpam.txt";

    int appCount = 0;
    int smsCount = 0;
    int mailCount = 0;
    int tutorCount = 0;
    int notutorCount = 0;

    @Override
    public void runTask() throws Exception {
        final File file = new File(SPAM_FILENAME);
        final List<String> lines;
        if (file.exists()) {
            //lines = Files.readAllLines(file.toPath());
            lines = new ArrayList<>();
        } else {
            lines = new ArrayList<>();
        }

        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(admissionProcess -> admissionProcess.getTitle().getContent().indexOf("2023") > 0)
                .filter(admissionProcess -> Utils.hasTutorDistribution(admissionProcess))
                .flatMap(admissionProcess -> admissionProcess.getAdmissionProcessTargetSet().stream())
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(application -> !lines.contains(application.getExternalId()))
                .forEach(application -> {
                    appCount++;
                    final Registration registration = Utils.registrationFor(application);
                    if (registration != null) {
                        final Tutorship tutorship = registration.getLastStudentCurricularPlan().getTutorshipsSet()
                                .stream().findAny().orElse(null);
                        if (registration.getLastStudentCurricularPlan().getTutorshipsSet().size() > 1) {
                            taskLog("Multipe Tutors for: %s%n", registration.getPerson().getUsername());
                        }

                        if (tutorship == null) {
                            notutorCount++;
                        } else {
                            tutorCount++;
                            final String name = StringFormatter.prettyPrint(registration.getPerson().getGivenNames());
                            final Gender gender = registration.getPerson().getGender();
                            final Gender tgender = tutorship.getTeacher().getPerson().getGender();
                            final String mobile = application.getAccount().getIdentity()
                                    .getAccountSet().stream()
                                    .filter(account -> account.getIsMobileValidated())
                                    .map(account -> account.getMobile())
                                    .findAny().orElse(null);
                            final String email = application.getAccount().getIdentity().getAccountSet().stream()
                                    .filter(a -> !a.getEmail().startsWith("deges"))
                                    .filter(a -> !a.isUsernameEmail())
                                    .map(a -> a.getEmail())
                                    .findAny().orElse(null);
                            lines.add(application.getExternalId());
                            if (mobile == null) {
                                taskLog("No mobile for: %s%n", application.getAccount().getEmail());
                                if (email == null) {
                                    taskLog("No mobile for: %s%n", application.getAccount().getEmail());
                                } else {
                                    mailCount++;
//                                    sendEmail(name, tutorship.getTeacher().getPerson().getUser().getProfile().getDisplayName(), email, gender, tgender);
                                }
                            } else {
                                smsCount++;
                                final String message = message(name, tutorship.getTeacher().getPerson().getUser().getProfile().getDisplayName(), gender, tgender);
                                if (SMSMessage.getInstance().sendSMS(mobile, message)) {
                                    taskLog("%s = %s%n", message.length(), message);
                                } else {
                                    taskLog("Failed SMS to: %s%n", email);
                                    sendEmail(name, tutorship.getTeacher().getPerson().getUser().getProfile().getDisplayName(), email, gender, tgender);
                                }
                            }
                        }

                    }
                });

        Files.write(file.toPath(), lines.stream().collect(Collectors.joining("\n")).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        taskLog("appCount = %s%n", appCount);
        taskLog("notutorCount = %s%n", notutorCount);
        taskLog("tutorCount = %s%n", tutorCount);
        taskLog("smsCount = %s%n", smsCount);
        taskLog("mailCount = %s%n", mailCount);
    }

    private void sendEmail(final String name, final String tutor, final String email, final Gender gender, final Gender tgender) {
        Message.fromSystem()
                .singleTos(email)
                .subject("Técnico Lisboa - Tutor")
                .textBody(messageMail(name, tutor, gender, tgender))
                .send();
    }

/*
    Agora que és estudante do Técnico Lisboa, tens uma Tutoria ativa com o/a docente <Nome do Tutor>. Consulta detalhes sobre a primeira reunião, que poderá ser após a sessão de acolhimento do teu curso, em https://nda.tecnico.ulisboa.pt/.
*/
 
    private String message(final String name, final String tutor, final Gender gender, final Gender tgender) {
        return "Car" + (gender == Gender.MALE ? "o" : "a") + " " + name + ", " +
                "agora que és estudante do Técnico Lisboa, "
                + (tgender == Gender.MALE ? "o teu tutor é o professor " : "a tue tutora é a professora ")
                + tutor + ". Consulta detalhes sobre a primeira reunião " +
                "em https://ist.pt/nda";
    };

    private String messageMail(final String name, final String tutor, final Gender gender, final Gender tgender) {
        return "Car" + (gender == Gender.MALE ? "o" : "a") + " " + name + ", \n\n" +
                "agora que és estudante do Técnico Lisboa, "
                + (tgender == Gender.MALE ? "o teu tutor é o professor" : "a tue tutora é a professora")
                + tutor + ".\n\nConsulta detalhes sobre a primeira reunião " +
                "em https://ist.pt/nda";
    };

    private Degree degreeFor(final AdmissionProcessTarget target) {
        final JsonObject config = target.getOutcomeConfigJson();
        return FenixFramework.getDomainObject(config.get("degree").getAsString());
    }

    private String campus(final Degree degree) {
        return degree.getCurrentCampus().stream()
                .map(c -> c.getName())
                .collect(Collectors.joining(", "));
    }

}