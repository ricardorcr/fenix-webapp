package pt.ist.fenix.webapp.task.academic;

import org.apache.poi.ss.usermodel.Row;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequest;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequestSituation;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequestSituationType;
import org.fenixedu.academic.domain.serviceRequests.documentRequests.DegreeFinalizationCertificateRequest;
import org.fenixedu.academic.dto.serviceRequests.AcademicServiceRequestBean;
import org.fenixedu.admissions.ist.util.SheetUtils;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class CloseDegreeFinalizationCertificateRequest extends CustomTask implements SheetUtils {

    @Override
    public void runTask() throws Exception {

//        final String filename = "/afs/ist.utl.pt/ciist/fenix/fenix015/Certidões para dar baixa no Fénix.xlsx";
        final String filename = "/home/rcro/Documents/fenix/academicos/Certidões para dar baixa no Fénix.xlsx";
        final byte[] content = Files.readAllBytes(new File(filename).toPath());
        final Stream<Row> students = xlsxRowStream(content, "final");

        final User responsible = User.findByUsername("ist23978");
        students.forEach(row -> {
            final String username = row.getCell(0).getStringCellValue();
            final String firstCycle = row.getCell(1).getStringCellValue();
            final String secondCycle = row.getCell(2).getStringCellValue();

            User user = null;
            if (username.startsWith("ist")) {
                user = User.findByUsername(username);
            } else {
                user = User.findByUsername("ist1" + username);
            }
            if (user == null) {
                taskLog("Can not find user: %s%n", username);
            } else {
                user.getPerson().getStudent().getRegistrationsSet().stream()
                        .flatMap(r -> r.getAcademicServiceRequestsSet().stream())
                        .filter(DegreeFinalizationCertificateRequest.class::isInstance)
                        .map(DegreeFinalizationCertificateRequest.class::cast)
                        .filter(request -> isRequestToClose(request, firstCycle, secondCycle))
                        .forEach(request -> deliver(request, responsible.getPerson()));
            }
        });
    }

    private boolean isRequestToClose(final DegreeFinalizationCertificateRequest request, final String firstCycle,
                                     final String secondCycle) {
        return (request.getRequestedCycle() == CycleType.FIRST_CYCLE && !firstCycle.isEmpty()) ||
                (request.getRequestedCycle() == CycleType.SECOND_CYCLE && !secondCycle.isEmpty());
    }

    final public void deliver(final DegreeFinalizationCertificateRequest academicServiceRequest,
                                final Person responsible) {
        AcademicServiceRequestBean concludedBean =
                new AcademicServiceRequestBean(AcademicServiceRequestSituationType.DELIVERED, responsible);
        concludedBean.setJustification("Alterado por script");
        concludedBean.setFinalSituationDate(new DateTime());

        edit(academicServiceRequest, concludedBean);
    }

    public void edit(final DegreeFinalizationCertificateRequest academicServiceRequest,
                     final AcademicServiceRequestBean academicServiceRequestBean) {

        if (!academicServiceRequest.isEditable()) {
            taskLog("Erro para o %s\t%s\tnot editable\t%s\t%s%n", academicServiceRequest.getPerson().getUsername(),
                    academicServiceRequest.getExternalId(), academicServiceRequest.getDescription(),
                    academicServiceRequest.getAcademicServiceRequestSituationType());
//            throw new DomainException("error.serviceRequests.AcademicServiceRequest.is.not.editable");
            return;
        }

        taskLog("Processing: %s\t%s\t%s\t%s%n", academicServiceRequest.getPerson().getUsername(),
                academicServiceRequest.getExternalId(), academicServiceRequest.getDescription(),
                academicServiceRequest.getAcademicServiceRequestSituationType());
        if (academicServiceRequest.getAcademicServiceRequestSituationType() != academicServiceRequestBean
                .getAcademicServiceRequestSituationType()) {
            checkRulesToChangeState(academicServiceRequest, academicServiceRequestBean
                    .getAcademicServiceRequestSituationType());
            internalChangeState(academicServiceRequest, academicServiceRequestBean);
            createAcademicServiceRequestSituation(academicServiceRequest, academicServiceRequestBean);
        } else {
            taskLog("Entrou no else! %s\t%s\t%s%n", academicServiceRequest.getPerson().getUsername(),
                    academicServiceRequest.getExternalId(), academicServiceRequest.getDescription());
//            academicServiceRequest.getActiveSituation().edit(academicServiceRequestBean);
        }

    }

    private void createAcademicServiceRequestSituation(
            final AcademicServiceRequest academicServiceRequest,
            final AcademicServiceRequestBean academicServiceRequestBean) {
        Constructor<AcademicServiceRequestSituation> constructor = null;
        try {
            constructor = AcademicServiceRequestSituation.class.getDeclaredConstructor(AcademicServiceRequest
                    .class, AcademicServiceRequestBean.class);
            constructor.setAccessible(true);
            constructor.newInstance(academicServiceRequest, academicServiceRequestBean);
        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException |
                 IllegalAccessException e1) {
            throw new RuntimeException(e1);
        }
    }


    protected void checkRulesToChangeState(final AcademicServiceRequest academicServiceRequest,
                                           final AcademicServiceRequestSituationType situationType) {

        if (!isAcceptedSituationType(academicServiceRequest, situationType)) {
            String sourceState =
                    academicServiceRequest.getActiveSituation().getAcademicServiceRequestSituationType().getLocalizedName();
            String targetState = situationType.getLocalizedName();

            throw new DomainException(
                    "error.serviceRequests.AcademicServiceRequest.cannot.change.from.source.state.to.target.state",
                    sourceState,
                    targetState);
        }
    }

    final protected boolean isAcceptedSituationType(final AcademicServiceRequest academicServiceRequest,
                                                    final AcademicServiceRequestSituationType situationType) {
        return getAcceptedSituationTypes(academicServiceRequest.getAcademicServiceRequestSituationType()).contains(situationType);
    }

    protected void internalChangeState(final AcademicServiceRequest academicServiceRequest,
                                       final AcademicServiceRequestBean academicServiceRequestBean) {
        verifyIsToDeliveredAndIsPayed(academicServiceRequest, academicServiceRequestBean);
    }

    protected void verifyIsToDeliveredAndIsPayed(final AcademicServiceRequest academicServiceRequest,
                                                 final AcademicServiceRequestBean academicServiceRequestBean) {
        if (academicServiceRequestBean.isToDeliver()) {
            if (academicServiceRequest.getEventType() != null && !isPayed(academicServiceRequest)) {
                throw new DomainException("AcademicServiceRequest.hasnt.been.payed");
            }
        }
    }

    protected boolean isPayed(final AcademicServiceRequest academicServiceRequest) {
        return academicServiceRequest.getEvent() == null || academicServiceRequest.getEvent().isPayed();
    }

    private List<AcademicServiceRequestSituationType> getAcceptedSituationTypes(AcademicServiceRequestSituationType
                                                                                        situationType) {

        return switch (situationType) {
            case NEW -> getNewSituationAcceptedSituationsTypes();
            case PROCESSING -> getProcessingSituationAcceptedSituationsTypes();
            case SENT_TO_EXTERNAL_ENTITY -> getSentToExternalEntitySituationAcceptedSituationsTypes();
            case RECEIVED_FROM_EXTERNAL_ENTITY -> getReceivedFromExternalEntitySituationAcceptedSituationsTypes();
            case CONCLUDED -> getConcludedSituationAcceptedSituationsTypes();
            default -> Collections.emptyList();
        };
    }

    protected List<AcademicServiceRequestSituationType> getNewSituationAcceptedSituationsTypes() {
        return List.of(AcademicServiceRequestSituationType.CANCELLED, AcademicServiceRequestSituationType.REJECTED,
                AcademicServiceRequestSituationType.PROCESSING);
    }

    protected List<AcademicServiceRequestSituationType> getProcessingSituationAcceptedSituationsTypes() {
        if (isPossibleToSendToOtherEntity()) {
            return List.of(AcademicServiceRequestSituationType.CANCELLED,
                    AcademicServiceRequestSituationType.REJECTED,
                    AcademicServiceRequestSituationType.SENT_TO_EXTERNAL_ENTITY);
        } else {
            return List.of(AcademicServiceRequestSituationType.CANCELLED,
                    AcademicServiceRequestSituationType.REJECTED, AcademicServiceRequestSituationType.CONCLUDED);
        }
    }

    protected List<AcademicServiceRequestSituationType> getSentToExternalEntitySituationAcceptedSituationsTypes() {
        return Collections
                .singletonList(AcademicServiceRequestSituationType.RECEIVED_FROM_EXTERNAL_ENTITY);
    }

    protected List<AcademicServiceRequestSituationType> getReceivedFromExternalEntitySituationAcceptedSituationsTypes
            () {
        return List.of(AcademicServiceRequestSituationType.CANCELLED, AcademicServiceRequestSituationType.REJECTED,
                AcademicServiceRequestSituationType.CONCLUDED);
    }

    protected List<AcademicServiceRequestSituationType> getConcludedSituationAcceptedSituationsTypes() {
        return List.of(AcademicServiceRequestSituationType.CANCELLED, AcademicServiceRequestSituationType.DELIVERED);
    }

    public boolean isPossibleToSendToOtherEntity() {
        return false;
    }
}
