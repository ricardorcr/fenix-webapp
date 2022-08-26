package pt.ist.fenix.webapp.task.admissions;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class AddMoreTargetsToNavyStuff extends CustomTask {

    protected static final Locale PT = new Locale("pt", "PT");
    protected static final Locale EN = new Locale("en", "GB");

    private static final Set<String> allowedNavyDegrees = new HashSet<String>() {{
        add("MEIC-A");
        add("MEIC-T");
        add("MEAN");
    }};

    @Override
    public void runTask() throws Exception {
        final AdmissionProcess admissionProcess = FenixFramework.getDomainObject("571432513831068");
        final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear().getNextExecutionYear();
        final IngressionType ingressionType = IngressionType.findIngressionTypeByCode("AD").get();
        final RegistrationProtocol protocol = Bennu.getInstance().getRegistrationProtocolsSet().stream()
                .filter(p -> p.getCode().equals("NORMAL"))
                .findAny().orElse(null);

        createTargets(
                admissionProcess,
                executionYear,
                ingressionType,
                protocol,
                allowedNavyDegrees,
                "CYCLE_2_NORMAL",
                CycleType.SECOND_CYCLE
        );
    }

    private Set<AdmissionProcessTarget> createTargets(final AdmissionProcess admissionProcess,
                                                      final ExecutionYear executionYear,
                                                      final IngressionType ingressionType,
                                                      final RegistrationProtocol registrationProtocol,
                                                      final Set<String> allowedDegrees,
                                                      final String eventTemplate,
                                                      final CycleType... cycleTypes) {

        return executionYear.getExecutionDegreesSet().stream()
                .map(ExecutionDegree::getDegree)
                .filter(degree -> degree.isFirstCycle() || degree.isSecondCycle())
                .filter(degree -> allowedDegrees.isEmpty() || allowedDegrees.contains(degree.getSigla()))
                .flatMap(degree -> Arrays.stream(cycleTypes)
                        .filter(cycleType -> degree.getCycleTypes().contains(cycleType))
                        .map(cycleType -> {
                            LocalizedString name = degree.getPresentationNameI18N(executionYear);
                            name = ls(name.getContent(PT), name.getContent(EN));

                            if (degree.getCycleTypes().size() > 1 && cycleTypes.length > 1) {
                                name = name.append(" ").append(cycleType == CycleType.FIRST_CYCLE
                                        ? ls("(1º Ciclo)", "(1st Cycle)") : ls("(2º Ciclo)", "(2nd Cycle)"));
                            }

                            final AdmissionProcessTarget admissionProcessTarget = admissionProcess.createAdmissionProcessTarget(name, null);

                            final JsonObject outcome = new JsonObject();
                            outcome.addProperty("degree", degree.getExternalId());
                            JsonUtils.addIf(outcome, "ingressionType", ingressionType != null ? ingressionType.getExternalId() : null);
                            outcome.addProperty("protocol", registrationProtocol.getExternalId());
                            outcome.addProperty("cycleType", cycleType.name());
                            outcome.addProperty("year", executionYear.getExternalId());
                            outcome.addProperty("eventTemplate", eventTemplate);
                            outcome.add("actionName", ls("Matricular", "Enroll").json());

                            admissionProcessTarget.setOutcomeConfig(outcome.toString());
                            return admissionProcessTarget;
                        }))
                .collect(Collectors.toSet());
    }

    private LocalizedString ls(final String pt, final String en) {
        return new LocalizedString(Locale.forLanguageTag("pt-PT"), pt).with(Locale.forLanguageTag("en-GB"), en);
    }

}