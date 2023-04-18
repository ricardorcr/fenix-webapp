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

    //TODO check hack at the end!

    static int count = 0;
    private final String[] FLUNKED_STUDENTS_TO_CORRECT = new String[]{
            "21975","63990","76315","79351","79516","81211","81884","82425",
            "86874","86930","88020","90952","78343","78411","81640"
    };

    private final String SUBJECT = "Levantamento de prescrição para o ano letivo 2022/2023";
    private final String BODY = "Caro aluno do TÉCNICO,\n" +
            "Após a atualização da sua informação académica e/ou pelo deferimento do recurso apresentado, o seu nome foi excluído da lista final de prescritos para 2022/2023.\n" +
            "Assim, poderá inscrever-se em unidades curriculares do 1º semestre do ano letivo 2022/2023, de 15 a 23 de setembro de 2022.\n" +
            "Apesar de já não constar da lista de prescrições em 2022/2023, salienta-se que o seu rendimento académico tem sido claramente abaixo do esperado. " +
            "Sabemos que vários são os motivos que podem ter condicionado o seu desempenho académico ao longo dos últimos anos. " +
            "Provavelmente já terá tentado inverter esta situação, o Núcleo de Desenvolvimento Académico (NDA) disponibiliza-se a traçar consigo um plano" +
            " específico e individualizado para melhorar o seu rendimento académico.\n" +
            "Por forma a evitar a sua prescrição nos próximos anos é aconselhado a contactar o NDA para:\n" +
            "1.    perceber as vantagens ou esclarecer dúvidas caso pretenda alterar a sua inscrição em 2022/2023 para o regime de “tempo parcial”.\n" +
            "2.    esclarecer qualquer questão que tenha relativa à Lei das Prescrições e às condições de exceção que evitaram a sua prescrição.\n" +
            "\n" +
            "\n" +
            "Com os melhores cumprimentos e votos de um bom ano escolar de 2022/2023,\n" +
            "Prof. Alexandre Francisco\n" +
            "Vice-Presidente para os Assuntos Académicos\n" +
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
                    if (student.getNumber() == 78343 || student.getNumber() == 78411 || student.getNumber() == 81640) {
                        //HACK check it each time!!! Remove condition after no longer needed
                        result = registration.getStartDate().isAfter(result.getStartDate()) ? result : registration;
                    } else {
                        throw new RuntimeException();
                    }
                }
            }
        }
        return result;
    }
}