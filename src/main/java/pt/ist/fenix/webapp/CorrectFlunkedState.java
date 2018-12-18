/**
 * Copyright © 2013 Instituto Superior Técnico
 * <p>
 * This file is part of FenixEdu IST Integration.
 * <p>
 * FenixEdu IST Integration is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * FenixEdu IST Integration is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.CustomGroup;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.messaging.core.domain.Message;
import org.fenixedu.messaging.core.domain.Sender;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

public class CorrectFlunkedState extends CustomTask {

    static int count = 0;
    private final String[] FLUNKED_STUDENTS_TO_CORRECT = new String[]{
            "45077", "53750", "56631", "58727", "63404", "69653", "74104",
            "74243", "76128", "76707", "78067", "78664", "78694", "80900",
            "82453", "84809", "84939", "85207", "88020"
    };

    private final String SUBJECT = "Levantamento de prescrição para o ano letivo 2019/2020";
    private final String BODY = "Caro aluno do TÉCNICO,\n" +
            "Após a apreciação do recurso apresentado, e por deferimento do mesmo, o \n" +
            "seu nome foi excluído da lista final de prescritos para 2019/2020.\n" +
            "Assim, poderá inscrever-se em unidades curriculares do 1º semestre do ano lectivo 2019/2020, \n" +
            "entre 9 e 13 de setembro de 2019.\n" +
            "\n" +
            "De qualquer forma, o seu rendimento académico tem sido claramente abaixo \n" +
            "do esperado. Sabemos que vários são os motivos que podem ter \n" +
            "condicionado o seu desempenho académico ao longo dos últimos anos. \n" +
            "Provavelmente já terá tentado inverter esta situação, o Núcleo de \n" +
            "Desenvolvimento Académico (NDA/GATu) disponibiliza-se a traçar consigo \n" +
            "um plano específico e individualizado para melhorar o seu rendimento \n" +
            "académico.\n" +
            "\n" +
            "Por forma a evitar a sua prescrição nos próximos anos é aconselhado a:\n" +
            "\n" +
            "   > contactar o NDA/GATu para:\n" +
            "\n" +
            "   - perceber as vantagens ou esclarecer dúvidas caso pretenda alterar a sua \n" +
            "inscrição em 2019/2020 para o regime de “tempo parcial”. Para mais \n" +
            "informações sobre o Regime de Tempo Parcial consulte o Guia Académico em \n" +
            "http://guiaacademico.tecnico.ulisboa.pt/1o-e-2o-ciclos-e-ciclos-integrados/regulamentos/regulamento-de-matriculas-e-inscricoes/\n" +
            "\n" +
            "   - esclarecer qualquer questão que tenha relativa à Lei das Prescrições e \n" +
            "às condições de exceção que evitaram a sua prescrição. Para mais \n" +
            "informações sobre a Lei das Prescrições consulte o regulamento de prescrições em \n" +
            "http://guiaacademico.tecnico.ulisboa.pt/1o-e-2o-ciclos-e-ciclos-integrados/regulamentos/regulamento-de-prescricoes/\n" +
            "\n" +
            "   > frequentar o Workshop “Para Prescrever a Prescrição”. Poderá inscrever-se ou consultar o Programa do Workshop em\n " +
            "https://nda.tecnico.ulisboa.pt/estudantes/atendimentoscoaching/monitorizacao-do-desempenho-academico/brac/." +
            "\n" +
            "\n" +
            "Com os melhores cumprimentos e votos de um bom ano escolar de 2019/2020,\n" +
            "\n" +
            "Professora Fátima Montemor\n" +
            "\n" +
            "Vice Presidente para os Assuntos Académicos,\n" +
            "\n" +
            "Conselho de Gestão do Instituto Superior Técnico";

    @Override
    public void runTask() throws Exception {
        final Sender sender = getConcelhoDeGestaoSender();

        for (int iter = 0; iter < FLUNKED_STUDENTS_TO_CORRECT.length; iter++) {

            final Student student = Student.readStudentByNumber(Integer.valueOf(FLUNKED_STUDENTS_TO_CORRECT[iter]));
            if (student == null) {
                taskLog("Can't find student -> " + FLUNKED_STUDENTS_TO_CORRECT[iter]);
                continue;
            }

            processStudent(sender, student);
        }
        taskLog("Modified: " + count);
    }

    private Sender getConcelhoDeGestaoSender() {
        return FenixFramework.getDomainObject("1696378937945247");
    }

    private void processStudent(final Sender sender, final Student student) {
        taskLog("Process Student -> " + student.getNumber());

        final Registration registration = getRegistrationWithFlunkedState(student);
        if (registration == null) {
            taskLog("\t- student is not in flunked state");
            return;
        }

        if (registration.getActiveStateType() != RegistrationStateType.REGISTERED) {
            RegistrationState registrationState =
                    RegistrationState.createRegistrationState(registration, null, new DateTime(),
                            RegistrationStateType.REGISTERED);
            registrationState.setRemarks("Prescrição levantada");
            taskLog("\t student modified");
            notifyStudent(sender, student);
        }
        count++;

        taskLog("*************************************");
    }

    private void notifyStudent(final Sender sender, final Student student) {
        String martaGracaUser = "ist24299";
        Message.from(sender).bcc(CustomGroup.users(student.getPerson().getUser()))
                .bcc(User.findByUsername(martaGracaUser).groupOf())
                .subject(SUBJECT)
                .textBody(BODY).send();
    }

    private Registration getRegistrationWithFlunkedState(final Student student) {
        Registration result = null;

        for (final Registration registration : student.getRegistrationsSet()) {
            if (registration.isBolonha() && registration.getActiveStateType() == RegistrationStateType.FLUNKED) {
                if (result == null) {
                    result = registration;
                } else {
                    taskLog("Student " + student.getNumber() + " has more than one flunked registrations");
                    throw new RuntimeException();
                }
            }
        }
        return result;
    }
}