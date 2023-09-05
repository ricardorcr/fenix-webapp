package pt.ist.fenix.webapp.task.tutor;

import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.ist.domain.RegistrationProcessState;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.ConnectSystem;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.messaging.core.domain.Message;
import org.joda.time.LocalDate;
import pt.ist.fenixedu.tutorship.domain.Tutorship;
import pt.ist.fenixedu.tutorship.domain.TutorshipIntention;
import pt.ist.fenixframework.FenixFramework;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FixFirstTimeStudentTutors extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final AdmissionProcess ap = FenixFramework.getDomainObject("2823232327516168"); //DGES 2023/2024
        final List<Registration> registrationsToAssign = new ArrayList<>();
        ap.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(app -> {
                    final Enum state = Utils.outcomeStateFor(app);
                    return state == RegistrationProcessState.CONFIRMED;
                })
                .map(Utils::registrationFor)
                .forEach(registration -> {
                    registration.getActiveStudentCurricularPlan().getTutorshipsSet().forEach(Tutorship::delete);
                    registrationsToAssign.add(registration);
                });

        registrationsToAssign.forEach(this::assignTutor);
    }

    private void assignTutor(final Registration registration) {
        final ExecutionDegree executionDegree = registration.getActiveStudentCurricularPlan().getDegreeCurricularPlan().getMostRecentExecutionDegree();
        TutorshipIntention tutorshipIntention = getBestTutorshipIntention(executionDegree);
        if (tutorshipIntention != null) {
            final Teacher teacher = tutorshipIntention.getTeacher();
            StudentCurricularPlan scp = registration.getActiveStudentCurricularPlan();
            if (scp == null || !scp.getTutorshipsSet().isEmpty()) {
                return;
            }
            int monthOfYear = new LocalDate().getMonthOfYear();
            final int tutorshipIntentionMonthOfYear = tutorshipIntention.getAcademicInterval().getBeginYearMonthDayWithoutChronology().getMonthOfYear();
            //tutorship uses dates so this is necessary to keep things coherent
            if (tutorshipIntentionMonthOfYear > monthOfYear) {
                monthOfYear = tutorshipIntentionMonthOfYear;
            }
            Tutorship.createTutorship(teacher, scp, monthOfYear, monthOfYear, Tutorship.getLastPossibleTutorshipYear());

            final Identity identity = registration.getPerson().getUser().getIdentity();
            final Account account = ConnectSystem.getMostRelevantAccount(identity);

            String body = "Caro/a " + registration.getPerson().getName() + ",\n" +
                    "\n" +
                    "Informamos que foi necessário gerar uma nova atribuição de tutor/a, pelo que agradecemos que não considere o e-mail anteriormente enviado. Este é então o e-mail que deve considerar relativamente à atribuição de Tutor/a. \n" +
                    "\n" +
                    "Agora que és estudante do Técnico Lisboa, tens uma Tutoria ativa.\n" +
                    "\n" +
                    "Consulta detalhes sobre a primeira reunião, que provavelmente vai ocorrer após a sessão de acolhimento do teu curso, em https://nda.tecnico.ulisboa.pt/.\n" +
                    "\n" +
                    "Os contactos do/a teu/tua Tutor/a\n" +
                    "\n" +
                    "Nome: " + teacher.getPerson().getName() + "\n" +
                    "E-mail: " + teacher.getPerson().getUser().getEmail() + "\n" +
                    "\n" +
                    "Podes verificar mais detalhes sobre a tua tutoria no portal de aluno no Fenix, através do seguinte link:  https://fenix.tecnico.ulisboa.pt/student/consult/tutor-info\n" +
                    "\n" +
                    "Bem-vindo ao Técnico, tira partido do Programa de Tutorado ao longo dos teus primeiros anos, ou sempre que precisares de um apoio mais individualizado." +
                    "\n\n\n" +
                    "Hello " + registration.getPerson().getName() + ",\n" +
                    "\n" +
                    "We would like to inform you that it was necessary to generate a new tutor assignment, so we kindly ask you to disregard the previously sent email. This is the email you should consider regarding the tutor assignment.\n" +
                    "\n" +
                    "Now that you are a student at Técnico Lisboa, you have been attributed a tutorship.\n" +
                    "\n" +
                    "To know more about your first meeting with your tutor, check for more detail here https://nda.tecnico.ulisboa.pt/.\n" +
                    "\n" +
                    "Your Tutor's contact:\n" +
                    "\n" +
                    "Name: " + teacher.getPerson().getName() + "\n" +
                    "E-mail: " + teacher.getPerson().getUser().getEmail() + "\n" +
                    "\n" +
                    "You can check more details about your tutorship on your students portal in Fenix. You can also follow this direct link: https://fenix.tecnico.ulisboa.pt/student/consult/tutor-info  \n" +
                    "\n" +
                    "Welcome to Técnico! Take advantage from the Tutoring Program throughout your first years, or whenever you need more personalized support." +
                    "\n\n" +
                    "----\n" +
                    "Núcleo de Desenvolvimento Académico";

            Message.fromSystem()
                    .singleTos(account.getEmail())
                    .subject("Reatribuição de Tutor/a - Reassigment of Tutor")
                    .textBody(body)
                    .send();
        }
    }

    private TutorshipIntention getBestTutorshipIntention(final ExecutionDegree executionDegree) {
        return TutorshipIntention.getTutorshipIntentions(executionDegree).stream()
                .min(Comparator.comparingInt(ti -> ti.getTutorships().size()))
                .orElse(null);
    }
}
