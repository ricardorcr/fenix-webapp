package pt.ist.fenix.webapp.task.accounting;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.accounting.Account;
import org.fenixedu.academic.domain.accounting.CustomEvent;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.AdmissionsISTConfiguration;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.LocalDate;
import pt.ist.fenixframework.FenixFramework;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CreatePhdStandaloneDebts extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Arrays.asList("ist193499", "ist196647", "ist196646").forEach(this::createDebts);
    }

    private void createDebts(final String username) {
        final User user = User.findByUsername(username);
        final Registration registration = user.getPerson().getStudent().getRegistrationsSet().stream()
                .filter(reg -> reg.getDegree().isThirdCycle())
                .findAny().get();
        final Account account = FenixFramework.getDomainObject("287762860391");
        final LocalDate dueDate = new LocalDate(2025,5,31);
        ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();

        final JsonObject applicationConfig = new JsonObject();
        LocalizedString description = BundleUtil.getLocalizedString(AdmissionsISTConfiguration.BUNDLE, "label.application.fee");
        applicationConfig.add("description", description.json());
        applicationConfig.addProperty("productCode", "0031");
        applicationConfig.addProperty("productDescription", "TAXAS DE MATRICULA");
        final Map<LocalDate, Money> applicationMap = new HashMap<>();
        applicationMap.put(dueDate, new Money(25));
        new CustomEvent(user.getPerson(), account, applicationMap, applicationConfig);

        if (!username.equals("ist193499")) {
            final JsonObject insuranceConfig = new JsonObject();
            LocalizedString insuranceDescription = BundleUtil.getLocalizedString(Bundle.STUDENT,
                    "label.custom.event.INSURANCE", currentExecutionYear.getName());
            insuranceConfig.add("description", insuranceDescription.json());
            insuranceConfig.addProperty("productCode", "0034");
            insuranceConfig.addProperty("productDescription", "SEGURO_ESCOLAR");
            insuranceConfig.addProperty("executionYear", currentExecutionYear.getExternalId());
            final Map<LocalDate, Money> insuranceDueDateMap = new HashMap<>();
            insuranceDueDateMap.put(dueDate, new Money(2.10));
            new CustomEvent(user.getPerson(), account, insuranceDueDateMap, insuranceConfig);

        }

        final JsonObject adminFeeConfig = new JsonObject();
        LocalizedString adminDescription = BundleUtil.getLocalizedString(Bundle.STUDENT,
                "label.custom.event.ADMIN_FEES", currentExecutionYear.getName());
        adminFeeConfig.add("description", adminDescription.json());
        adminFeeConfig.addProperty("productCode", "0031");
        adminFeeConfig.addProperty("productDescription", "TAXAS DE MATRICULA");
        adminFeeConfig.addProperty("type", "ADMIN_FEES");
        final Map<LocalDate, Money> adminDueDateMap = new HashMap<>();
        adminDueDateMap.put(dueDate, new Money(30));

        final BigDecimal penalty = new BigDecimal(15.0);
        final JsonObject penaltyMap = new JsonObject();
        penaltyMap.addProperty("31/05/2025", penalty);
        adminFeeConfig.add("penaltyAmountMap", penaltyMap);
        new CustomEvent(user.getPerson(), account, adminDueDateMap, adminFeeConfig);


        ExecutionSemester currentSemester = ExecutionSemester.readActualExecutionSemester();
        final Enrolment enrolment = registration.getStudentCurricularPlansSet().stream()
                .flatMap(StudentCurricularPlan::getEnrolmentStream)
                .findAny().get();
        final String semesterDescription = currentSemester.getName() + " " + currentExecutionYear.getName();

        final JsonObject standaloneFeeConfig = new JsonObject();
        LocalizedString standaloneDescription = BundleUtil.getLocalizedString(Bundle.STUDENT, "label.custom.event.TUITION",
                enrolment.getCurricularCourse().getName(), semesterDescription);
        standaloneFeeConfig.add("description", standaloneDescription.json());
        standaloneFeeConfig.addProperty("productCode", "0076");
        standaloneFeeConfig.addProperty("productDescription", "PROPINAS OUTROS");
        standaloneFeeConfig.addProperty("type", "TUITION");
        standaloneFeeConfig.addProperty("applyInterest", "true");
        standaloneFeeConfig.addProperty("applyInterest", "true");
        standaloneFeeConfig.addProperty("executionSemester", currentSemester.getExternalId());
        standaloneFeeConfig.addProperty("curricularCourse", "CC:getExternalId()");
        standaloneFeeConfig.addProperty("registrationDataByExecutionYear",
                registration.getRegistrationDataByExecutionYearSet().iterator().next().getExternalId());
        final Map<LocalDate, Money> standaloneDueDateMap = new HashMap<>();
        standaloneDueDateMap.put(dueDate, new Money(1350));
        new CustomEvent(user.getPerson(), account, standaloneDueDateMap, standaloneFeeConfig);

    }
}
