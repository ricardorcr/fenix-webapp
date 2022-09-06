package pt.ist.fenix.webapp.service;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.EventTemplate;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import pt.ist.fenix.webapp.config.academic.accounting.EventConfig;

public class EventTemplateService {

    public static void initEventTemplate(final Registration registration, final ExecutionYear executionYear) {
        if (registration.getEventTemplate() == null) {
            final EventTemplate eventTemplate = findBestTemplateFor(registration);
            if (eventTemplate == null) {
            } else {
                registration.setEventTemplate(eventTemplate);
            }
        }

        if (registration.isInMobilityState()) {
            if (executionYear != null) {
                RegistrationDataByExecutionYear.getOrCreateRegistrationDataByYear(registration, executionYear);
            }
        }
    }

    private static EventTemplate findBestTemplateFor(final Registration registration) {
        final RegistrationProtocol protocol = registration.getRegistrationProtocol();
        final Degree degree = registration.getDegree();
        final boolean isAlien = protocol.isAlien();
        final boolean isEmptyDegree = degree.isEmpty();
        final EventConfig.EventTemplateCode code;
        if (isEmptyDegree) {
            final boolean hasOtherRegistration = registration.getStudent().getRegistrationsSet().stream()
                    .filter(r -> r != registration)
                    .flatMap(r -> r.getStudentCurricularPlansSet().stream())
                    .flatMap(scp -> scp.getEnrolmentStream())
                    .anyMatch(enrolment -> enrolment.getExecutionYear().isCurrent());
            if (hasOtherRegistration) {
                code = isAlien ? EventConfig.EventTemplateCode.ISOLATED_COURSES_INTERNAL_INTERNATIONAL
                        : EventConfig.EventTemplateCode.ISOLATED_COURSES_INTERNAL;
            } else {
                code = EventConfig.EventTemplateCode.ISOLATED_COURSES_EXTERNAL;
            }
        } else if (protocol.isMilitaryAgreement()) {
            code = EventConfig.EventTemplateCode.MILITARY;
        } else if (isAlien) {
            code = degree.getSigla().equals("MOTU") ? EventConfig.EventTemplateCode.INTERNATIONAL_MOTU : EventConfig.EventTemplateCode.INTERNATIONAL;
        } else if (protocol.isMobilityAgreement()) {
            code = null;
        } else {
            code = EventConfig.degreeEventTemplateMap().get(degree);
        }
        return code == null ? null : code.eventTemplate();
    }

}
