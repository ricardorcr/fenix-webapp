package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.CustomGroup;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.messaging.core.domain.Message;
import org.fenixedu.messaging.core.domain.Sender;
import pt.ist.fenixframework.FenixFramework;

import java.util.HashSet;
import java.util.Set;

public class SendEmailToFlunkedStudents extends CustomTask {

    private static final String[] FLUNKED_STUDENTS = new String[]{"37090","44637","50368","51410","55314","63544","63916","65652","67377","68252","74328","78072",
            "78343","78781","79728","81400","81941","86844","86930","87532","88042","89312","89481","89862","90473","90750","90920","92516","92758","92973","94133",
            "97469","98945","98961","99419","99845","100667","100750","101214","102360","102420","103130","103230","103448","103628","103707","103855","104007",
            "104053","104087","104094","104096","104100","104107","104143","104144","104618"};

    @Override
    public void runTask() throws Exception {
        User user = User.findByUsername("ist24616");
        Authenticate.mock(user, "Script SendEmailToFlunkedStudents");

        Set<User> students = new HashSet<User>();
        for (int iter = 0; iter < FLUNKED_STUDENTS.length; iter++) {

            Student student = Student.readStudentByNumber(Integer.valueOf(FLUNKED_STUDENTS[iter]));
            if (student == null) {
                taskLog("Can't find student -> " + FLUNKED_STUDENTS[iter]);
                continue;
            }
            students.add(student.getPerson().getUser());
        }

        createEmail(students);
        taskLog("Done.");
    }

    private void createEmail(final Set<User> students) {

        final Sender sender = getConcelhoDeGestaoSender();

        String martaGracaUser = "ist24299";
        Message.from(sender).bcc(CustomGroup.users(students.stream()))
                .bcc(User.findByUsername(martaGracaUser).groupOf())
                .subject(getSubject())
                .textBody(getBody()).send();
        taskLog("Sent: " + students.size() + " emails");
    }

    private Sender getConcelhoDeGestaoSender() {
        return FenixFramework.getDomainObject("1696378937945247");
    }

    private String getBody() {
        final StringBuilder body = new StringBuilder();

        //  Mail quando os alunos são retirados da lista de prescristos
//        body.append("Caro estudante do TÉCNICO,\n");
//        body.append("\n");
//        body.append("Após análise do seu currículo académico, verificou-se estar numa das situações previstas para prescrição no Regulamento de Prescrições do IST (https://tecnico.ulisboa.pt/pt/ensino/estudar-no-tecnico/informacoes-academicas/).\n");
//        body.append("\n");
//        body.append("Na lista definitiva, apurada no dia 15 de setembro, tem a atualização da sua situação académica tendo em conta as notas entretanto lançadas.\n");
//        body.append("\n");
//        body.append("Os alunos sujeitos a prescrição não poderão efetuar a sua inscrição em unidades curriculares no ano letivo 2022/2023.\n");
//        body.append("\n");
//        body.append("O rendimento académico tem sido claramente abaixo do esperado. Sabemos que vários são os motivos que podem ter condicionado o seu desempenho académico ao longo dos últimos anos. Provavelmente já terá tentado inverter esta situação, o Núcleo de Desenvolvimento Académico (NDA) disponibiliza-se a traçar consigo um plano específico e individualizado para melhorar o seu rendimento académico.\n");
//        body.append("Por forma a evitar a sua prescrição nos próximos anos é aconselhado a contactar o NDA para:\n");
//        body.append("1.    perceber as vantagens ou esclarecer dúvidas caso pretenda futuramente fazer a sua inscrição em regime de “tempo parcial”.\n");
//        body.append("2.    esclarecer qualquer questão que tenha relativa à Lei das Prescrições e às condições de exceção que evitarão que volte a prescrever.\n");
//        body.append("\n");
//        body.append("Com os melhores cumprimentos,\n");
//        body.append("Prof. Alexandre Francisco\n");
//        body.append("Vice-Presidente para os Assuntos Académicos\n");
//        body.append("Conselho de Gestão do Instituto Superior Técnico\n");


        //Mail quando são postos como prescritos
        body.append("Caro Aluno do TÉCNICO,\n");
        body.append("\n");
        body.append("Após análise do seu currículo académico, verificou-se estar numa das situações previstas para prescrição no Regulamento de Prescrições do IST (disponível em https://tecnico.ulisboa.pt/pt/ensino/estudar-no-tecnico/informacoes-academicas/inscricoes/).\n\n");
        body.append("Os alunos sujeitos a prescrição não poderão efetuar a sua inscrição em unidades curriculares no ano letivo 2024/2025.\n\n");
        body.append("O seu rendimento académico tem sido claramente abaixo do esperado. Sabemos que vários são os motivos que podem ter condicionado o seu desempenho académico ao longo dos últimos anos. O Núcleo de Desenvolvimento Académico (NDA) disponibiliza-se a traçar consigo um plano específico e individualizado para melhorar o seu rendimento académico.\n");
        body.append("Por forma a evitar a melhorar o seu rendimento académico no futuro aconselhamos a contactar o NDA para:\n");
        body.append("1. perceber as vantagens ou esclarecer dúvidas caso pretenda futuramente fazer a sua inscrição em regime de “tempo parcial”.\n");
        body.append("2.    esclarecer qualquer questão que tenha relativa à Lei das Prescrições e às condições de exceção que evitarão que volte a prescrever.\n");
        body.append("\n");
        body.append("\n");
        body.append("Com os melhores cumprimentos,\n");
        body.append("Prof. Francisco Melo\n");
        body.append("Vice-Presidente para os Assuntos Académicos\n");
        body.append("Conselho de Gestão do Instituto Superior Técnico\n");

        return body.toString();
    }

    private String getSubject() {
        return "Prescrição para o ano lectivo 2024/2025";
    }
}