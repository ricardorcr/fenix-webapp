package pt.ist.fenix.webapp.task.admissions;

import com.qubit.solution.fenixedu.integration.cgd.services.form43.CgdForm43Sender;
import org.apache.commons.lang.BooleanUtils;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.papyrus.service.PapyrusPdfRendererService;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixedu.integration.domain.cgd.CgdCard;
import pt.ist.fenixedu.integration.ui.spring.service.RegistrationDeclarationForBanksService;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.papyrus.PapyrusClient;
import pt.ist.papyrus.PapyrusConfiguration;
import pt.ist.papyrus.PapyrusSettings;
import pt.ist.registration.process.ui.service.RegistrationDeclarationDataProvider;

public class ResendCGDForms extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getCgdCardCounterSet().stream()
                .filter(counter -> counter.getYear() == 2022)
                .flatMap(counter -> counter.getCgdCardSet().stream())
                .filter(card -> card.getSuccessfulSentData() != null)
                .filter(card -> card.getSuccessfulSentData().getMonthOfYear() == 9)
                .peek(card -> taskLog("Processing: %s%n", card.getUser().getUsername()))
                .forEach(cgdCard -> {
                    final CgdRunnable cgdRunnable = new CgdRunnable(cgdCard);
                    cgdRunnable.run();
                });
    }

    private class CgdRunnable implements Runnable {

        RegistrationDeclarationForBanksService registrationDeclarationForBanksService = new RegistrationDeclarationForBanksService(
                new RegistrationDeclarationDataProvider(),
                new PapyrusPdfRendererService(new PapyrusClient(PapyrusConfiguration.getConfiguration().papyrusUrl(),
                        PapyrusConfiguration.getConfiguration().papyrusToken()), PapyrusSettings.newBuilder().build()));

        private CgdCard card;
        public CgdRunnable(CgdCard card) {
            this.card = card;
        }

        @Atomic(mode = Atomic.TxMode.READ)
        public void run() {
            if (this.card == null) {
                taskLog("CGD: Não existe cartão para este pedido.");
                return;
            }
            final Person person = this.card.getUser().getPerson();
            final String username = this.card.getUser().getUsername();
            if (BooleanUtils.isTrue(this.card.getAllowSendBankDetails())) {
                if (person != null) {
                    final Student student = person.getStudent();
                    if (student != null) {
                        for (final Registration registration : student.getRegistrationsSet()) {
                            if (registration.isActive()) {
                                CgdForm43Sender sender = new CgdForm43Sender();
                                boolean form = sender.sendForm43For(registration);
                                boolean attachment = sender.uploadFormAttachment(registration, registrationDeclarationForBanksService
                                        .getRegistrationDeclarationFileForBanks(registration));
                                taskLog("Sent Form43 ({}) and registration declaration file ({}) for registration {}",
                                        form, attachment, registration.getExternalId() );
                                if (form && attachment) {
                                    FenixFramework.atomic(() -> card.setSuccessfulSentData(new DateTime()));
                                    taskLog(String.format("CGD: Comunicação efectuada à CGD com sucesso para o utilizador %s", username));
                                    return;
                                } else {
                                    taskLog(String.format("CGD: Comunicação falhou para o utilizador %s. Contactar a CGD.", username));
                                    return;
                                }
                            }
                        }
                        taskLog(String.format("CGD: Não existe uma matrícula activa para o aluno %s", username));
                        return;
                    }
                    taskLog(String.format("CGD: Utilizador %s não é aluno", username));
                    return;
                } else {
                    taskLog(String.format("CGD: Utilizador %s não tem pessoa activa", username));
                    return;
                }
            }
            taskLog(String.format("CGD: %s - É necessário autorização a cedência de dados à CGD para efeitos de abertura de conta", username));
        }
    }

}