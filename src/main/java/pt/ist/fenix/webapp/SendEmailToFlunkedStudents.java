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

    private static final String[] FLUNKED_STUDENTS = new String[]{"67861", "68354", "74265", "76163", "76537", "76782", "77076", "78072", "78343", "79838",
            "80939", "81060", "81183", "81400", "81495", "81884", "82425", "82605", "83659", "83953", "85326", "86289", "86422", "86844", "87238", "87676", "87685",
            "88020", "89192", "90079", "90952", "90953", "91162", "91188", "94243", "94325"};

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
        body.append("Após análise do seu currículo académico, verificou-se estar numa das situações previstas para prescrição no Regulamento de Prescrições do IST (disponível em https://tecnico.ulisboa.pt/pt/ensino/estudar-no-tecnico/informacoes-academicas/inscricoes/).\n");
        body.append("A lista provisória de alunos a prescrever encontra-se afixada, junto dos Serviços Académicos, desde o dia 17 de agosto de 2023.\n");
        body.append("Na lista definitiva, a publicar no dia 4 de setembro, será atualizada a situação académica do aluno tendo em conta as notas entretanto lançadas. Além disso, de acordo com o Regulamento de prescrições do IST, no ponto 5 prevê-se: \"Atento ainda o princípio da proporcionalidade, consagrado constitucional e legalmente, poderá ainda a aplicação de regras de prescrição ser ajustada a casos em que, invocados e inequivocamente provados pelo aluno, este, por motivos de força maior, se viu impossibilitados de frequentar as atividades letivas e assim alcançar um nível mínimo de aproveitamento escolar. Tal ajuste será efetuado mediante requerimento dirigido ao presidente do IST e entregue na Área de Graduação — Alameda, ou Área de Gestão de Recursos Humanos e Académicos do Taguspark.\", sendo o prazo estabelecido para o efeito, de 17 a 25 de agosto de 2023.\n");
        body.append("No caso de alteração da situação de prescrição, quer por via de atualização de notas quer no caso de deferimento ao requerimento apresentado, a inscrição em unidades curriculares no 1º semestre do ano letivo 2023/2024 poderá ser efetuada de 04 a 10 de setembro de 2023.\n");
        body.append("Os alunos sujeitos a prescrição não poderão efetuar a sua inscrição em unidades curriculares no ano letivo 2023/2024.\n");
        body.append("\n");
        body.append("\n");
        body.append("Com os melhores cumprimentos,\n");
        body.append("Prof. Alexandre Francisco\n");
        body.append("Vice-Presidente para os Assuntos Académicos\n");
        body.append("Conselho de Gestão do Instituto Superior Técnico\n");

        return body.toString();
    }

    private String getSubject() {
        return "Prescrição para o ano lectivo 2023/2024";
    }
}