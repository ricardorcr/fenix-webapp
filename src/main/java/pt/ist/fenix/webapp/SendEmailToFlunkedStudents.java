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

    private static final String[] FLUNKED_STUDENTS = new String[] { "37090","52997","63108","65272","65504","65883","67253","68130","69389","69613","70939","73358","73708","73834","74123",
    		"74139","74243","76163","76345","76707","77076","78001","78067","78127","78343","78411","78694","78753","78991","79666","80900","80987","81211","81312","81640","81997","82013",
    		"82244","82541","83983","84770","84777","84974","85115","85338","86357" };

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
//        body.append("Caro Aluno do IST,\n");
//        body.append("\n");
//        body.append("Após a reavaliação dos currículos académicos dos alunos prescritos em 16 de Agosto de 2013 (com base no lançamento de notas decorrido após esta data) e a apreciação dos recursos apresentados, verificou-se que o seu nome foi excluído da lista final de prescritos para 2013/2014.\n");
//        body.append("\n");
//        body.append("Assim, e se ainda não estiver inscrito em unidades curriculares no 1º semestre do ano lectivo 2013/14, poderá inscrever-se entre 10 e 13 de Setembro de 2013 na Internet (sistema Fénix) e no dia 16 de Setembro de 2013 nos Serviços Académicos.\n");
//        body.append("\n");
//        body.append("IST, 9 de Setembro de 2013\n");
//        body.append("\n");
//        body.append("O Conselho de Gestão do IST\n");


        //Mail quando são postos como prescritos
        body.append("Caro Aluno do TÉCNICO,\n");
        body.append("\n");
        body.append(
        		"Após a aplicação da Lei nº 38/2020, de 18 de agosto, acerca de “Medidas excecionais e temporárias para salvaguarda dos direitos dos trabalhadores e estudantes do ensino superior público”, analisámos o seu currículo académico, e comprovou-se estar numa das situações previstas para prescrição no Regulamento de Prescrições do IST (disponível em http://guiaacademico.tecnico.ulisboa.pt/1o-e-2o-ciclos-e-ciclos-integrados/regulamentos/regulamento-de-prescricoes/).\n");
        body.append("\n");
        body.append("A lista provisória de alunos a prescrever encontra-se afixada, junto dos Serviços Académicos, desde o dia 24 de agosto de 2020.\n");
        body.append("Na lista definitiva, a publicar no dia 14 de setembro, será atualizada a situação académica do aluno tendo em conta as notas entretanto lançadas. Além disso, de acordo com o Regulamento de prescrições do IST, no ponto 5 prevê-se: \"Atento ainda o princípio da proporcionalidade, consagrado constitucional e legalmente, poderá ainda a aplicação de regras de prescrição ser ajustada a casos em que, invocados e inequivocamente provados pelo aluno, este, por motivos de força maior, se viu impossibilitados de frequentar as actividades lectivas e assim alcançar um nível mínimo de aproveitamento escolar. Tal ajuste será efectuado mediante requerimento dirigido ao presidente do IST e entregue no Núcleo de Graduação — Alameda, ou Núcleo Académico do Taguspark.\", sendo o prazo estabelecido para o efeito, de 31 de agosto a 4 de setembro de 2020.\n\n");
        body.append("No caso de alteração da situação de prescrição, quer por via de atualização de notas quer no caso de deferimento ao requerimento apresentado, a inscrição em unidades curriculares no 1º semestre do ano letivo 2020/2021 poderá ser efetuada entre 14 e 18 de setembro de 2020.\n\n");
        body.append("Os alunos sujeitos a prescrição não poderão efetuar a sua inscrição em unidades curriculares no ano letivo 2020/2021.\n");
        body.append("\n");        
        body.append("\n");
        body.append("Com os melhores cumprimentos,\n");
        body.append("Prof. Alexandre Francisco\n");
        body.append("Vice-Presidente para os Assuntos Académicos\n");
        body.append("Conselho de Gestão do Instituto Superior Técnico\n");

        return body.toString();
    }

    private String getSubject() {
        return "Prescrição para o ano lectivo 2020/2021 – aplicação das “Medidas excecionais e temporárias para salvaguarda dos direitos dos trabalhadores e estudantes do ensino superior público” da Lei nº38/2020 de 18 de agosto";
    }
}