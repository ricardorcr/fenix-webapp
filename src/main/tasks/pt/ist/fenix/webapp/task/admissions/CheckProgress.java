package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.academic.domain.DomainOperationLog;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.MobilityProcessState;
import org.fenixedu.admissions.ist.domain.RegistrationProcessState;
import org.fenixedu.admissions.ist.domain.UserAccountInfo;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.connect.domain.Identity;
import pt.ist.fenixedu.integration.domain.CardDataAuthorizationLog;

import java.io.ByteArrayOutputStream;
import java.util.stream.Stream;

public class CheckProgress extends ReadCustomTask {

    private class Counter {

        long admitted = 0l;
        long boarding = 0l;
        long pendingConfirmation = 0l;
        long approved = 0l;
        long confirmed = 0l;
        long rejected = 0l;
        long hasUser = 0l;
        long hasEmail = 0l;
        long hasPassword = 0l;
        long hasDataAuthorization = 0l;
        long hasSurvey = 0l;
        long hasSlotForDocumentConfirmation = 0l;
        long hasSlotForCampusVisit = 0l;

        private Counter(final Stream<Application> stream) {
            stream.forEach(application -> {
                if (application.getAdmitted()) {
                    admitted++;

                    final Enum e = Utils.outcomeStateFor(application);
                    if (e == RegistrationProcessState.BOARDING || e == MobilityProcessState.BOARDING) {
                        boarding++;
                    }
                    if (e == MobilityProcessState.SUBMITTED_FOR_APPROVAL) {
                        pendingConfirmation++;
                    }
                    if (e == MobilityProcessState.APPROVED) {
                        approved++;
                    }
                    if (e == MobilityProcessState.REJECTED) {
                        rejected++;
                    }
                    if (e == RegistrationProcessState.REGISTERED) {
                        pendingConfirmation++;
                    }
                    if (e == RegistrationProcessState.CONFIRMED || e == MobilityProcessState.CONFIRMED) {
                        confirmed++;
                    }

                    final Identity identity = application.getAccount().getIdentity();
                    if (identity != null) {
                        final User user = identity.getUser();
                        if (user != null) {
                            hasUser++;

                            if (user.getPerson().getInstitutionalEmailAddress() != null) {
                                hasEmail++;
                            }

                            final UserAccountInfo userAccountInfo = user.getUserAccountInfo();
                            if (userAccountInfo != null) {
                                if (userAccountInfo.isPasswordSet()) {
                                    hasPassword++;
                                }
                            }

                            final long dataAuthRespo = user.getPerson().getDomainOperationLogsSet().stream()
                                    .filter(CardDataAuthorizationLog.class::isInstance)
                                    .sorted(DomainOperationLog.COMPARATOR_BY_WHEN_DATETIME.reversed())
                                    .map(CardDataAuthorizationLog.class::cast)
                                    .count();
                            if (dataAuthRespo > 4l) {
                                hasDataAuthorization++;
                            }
                        }
                    }
                }
            });
        }

        private Counter(final AdmissionProcess admissionProcess) {
            this(admissionProcess.getAdmissionProcessTargetSet().stream().
                    flatMap(target -> target.getApplicationSet().stream()));
        }

    }

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("StatusReport");

        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(this::include)
                .forEach(admissionProcess -> {
                    final Counter counter = new Counter(admissionProcess);

                    final Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("Instância", admissionProcess.getTitle().getContent());
                    row.setCell("Colocados", Long.toString(counter.admitted));
                    row.setCell("A Embarcar", Long.toString(counter.boarding));
                    row.setCell("Pendentes de Confirmação", Long.toString(counter.pendingConfirmation));
                    row.setCell("Approvados", Long.toString(counter.approved));
                    row.setCell("Confirmados", Long.toString(counter.confirmed));
                    row.setCell("Rejeitados", Long.toString(counter.rejected));
                    row.setCell("Com Utilizador", Long.toString(counter.hasUser));
                    row.setCell("Com E-mail", Long.toString(counter.hasEmail));
                    row.setCell("Com Password", Long.toString(counter.hasPassword));
                    row.setCell("Com Autorização de Dados", Long.toString(counter.hasDataAuthorization));
                    row.setCell("Inquérito Respondido", Long.toString(counter.hasSurvey));
                    row.setCell("Com Slot Confirmação Documentos", Long.toString(counter.hasSlotForDocumentConfirmation));
                    row.setCell("Com Slot Visita Campus", Long.toString(counter.hasSlotForCampusVisit));
                });

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("StatusReport.xlsx", stream.toByteArray());
    }

    private boolean include(final AdmissionProcess admissionProcess) {
        return admissionProcess.getTitle().getContent().indexOf("2023") > 0;
    }

}