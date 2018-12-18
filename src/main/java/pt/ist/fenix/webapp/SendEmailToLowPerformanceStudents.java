package pt.ist.fenix.webapp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.CustomGroup;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.messaging.core.domain.Message;
import org.fenixedu.messaging.core.domain.Sender;

import pt.ist.fenixframework.FenixFramework;

public class SendEmailToLowPerformanceStudents extends CustomTask {

    @Override
    public void runTask() throws Exception {
        User user = User.findByUsername("ist24616");
        Authenticate.mock(user, "Script SendEmailToLowPerformanceStudents");

        List<String> studentNumbers = null;
		try {
			studentNumbers = Files.readAllLines(new File("/afs/ist.utl.pt/ciist/fenix/fenix015/ist/Lista_alunos_baixo_rendimento_de_24-08-2020.txt").toPath());
		} catch (IOException e) {
			throw new Error("Erro a ler o ficheiro.");
		}
		Set<User> students = new HashSet<User>();
        for (String studentNumber : studentNumbers) {
            Student student = Student.readStudentByNumber(Integer.valueOf(studentNumber));
            if (student == null) {
                taskLog("Can't find student -> " + studentNumber);
                continue;
            }
            students.add(student.getPerson().getUser());
        }

        createEmail(students);

        taskLog("Done.");
    }

    private void createEmail(final Set<User> students) {

        Sender sender = FenixFramework.getDomainObject("1133428984512749"); //GATu

        String martaGracaUser = "ist24299";
        Message.from(sender).bcc(CustomGroup.users(students.stream()))
                .bcc(User.findByUsername(martaGracaUser).groupOf())
                .subject(getSubject())
                .textBody(getBody()).send();
        taskLog("Sent: " + students.size() + " emails");
    }

    private String getSubject() {
        return "Baixo rendimento académico";
    }

    private String getBody() {
        StringBuilder builder = new StringBuilder();

        builder.append("Caro aluno do TÉCNICO,\n\n");
        builder.append("Apesar de não constar da lista de prescrições em 2020/2021, verificou-se que o seu rendimento académico tem sido claramente abaixo do esperado. "
                + "Sabemos que vários são os motivos que podem ter condicionado o seu desempenho académico ao longo dos últimos anos. "
                + "Provavelmente já terá tentado inverter esta situação, o Núcleo de Desenvolvimento Académico (NDA) disponibiliza-se a traçar consigo um plano específico e individualizado para melhorar o seu rendimento académico.\n");
        builder.append("\n");
        builder.append("Por forma a evitar a sua prescrição nos próximos anos é aconselhado a contactar o NDA para:\n\n");
        builder.append("o perceber as vantagens ou esclarecer dúvidas caso pretenda alterar a sua inscrição em 2020/2021 para o regime de \"tempo parcial\"."
                + " Para mais informações sobre o Regime de Tempo Parcial consulte o Guia Académico em http://guiaacademico.tecnico.ulisboa.pt/1o-e-2o-ciclos-e-ciclos-integrados/regulamentos/regulamento-de-matriculas-e-inscricoes/."
                + " Para mais informações sobre a Lei das Prescrições consulte o regulamento de prescrições em http://guiaacademico.tecnico.ulisboa.pt/1o-e-2o-ciclos-e-ciclos-integrados/regulamentos/regulamento-de-prescricoes/\n\n");
        builder.append("o frequentar o Workshop “Para Prescrever a Prescrição”. Poderá inscrever-se ou consultar o Programa do Workshop em https://nda.tecnico.ulisboa.pt/estudantes/atendimentoscoaching/monitorizacao-do-desempenho-academico/brac/.\n");
        builder.append("\n");
        builder.append("\n");
        builder.append("Com os melhores cumprimentos e votos de um bom ano escolar de 2020/2021,\n");
        builder.append("Prof. Alexandre Francisco\n");
        builder.append("Vice-Presidente para os Assuntos Académicos\n");
        builder.append("Conselho de Gestão do Instituto Superior Técnico\n");

        return builder.toString();
    }
}