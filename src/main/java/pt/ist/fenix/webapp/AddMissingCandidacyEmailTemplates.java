package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.candidacyProcess.mobility.MobilityEmailTemplate;
import org.fenixedu.academic.domain.candidacyProcess.mobility.MobilityEmailTemplateType;
import org.fenixedu.academic.domain.candidacyProcess.mobility.MobilityProgram;
import org.fenixedu.academic.domain.period.MobilityApplicationPeriod;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AddMissingCandidacyEmailTemplates extends CustomTask {

    @Override
    public void runTask() throws Exception {
        MobilityApplicationPeriod mobilityApplicationPeriod = FenixFramework.getDomainObject("568202698424325");
        Map<MobilityEmailTemplateType, MobilityEmailTemplate> templateMap = new HashMap<>();

        //create map with a template for each type
        for (MobilityProgram mobilityProgram : mobilityApplicationPeriod.getMobilityPrograms()) {
            for (MobilityEmailTemplate template : mobilityApplicationPeriod.getEmailTemplatesSet()) {
                Arrays.asList(MobilityEmailTemplateType.values()).stream()
                        .filter(mett -> template.isFor(mobilityProgram, mett))
                        .forEach(mett -> templateMap.putIfAbsent(mett, template));
            }
            if (templateMap.keySet().size() == MobilityEmailTemplateType.values().length) {
                break;
            }
        }

        //create template for the ones that don't have
        for (MobilityProgram mobilityProgram : mobilityApplicationPeriod.getMobilityPrograms()) {
            Arrays.asList(MobilityEmailTemplateType.values()).stream()
                    .forEach(mett -> {
                        MobilityEmailTemplate template = getTemplate(mobilityApplicationPeriod, mobilityProgram, mett);
                        if (template == null) {
                            MobilityEmailTemplate emailTemplate = templateMap.get(mett);
                            if (emailTemplate != null) {
                                MobilityEmailTemplate.create(mobilityApplicationPeriod, mobilityProgram, mett,
                                        emailTemplate.getSubject(), emailTemplate.getBody());
                                taskLog("Created template for %s for type %s%n", mobilityProgram.getName(), mett);
                            }
                        }
                    });
        }
    }

    private MobilityEmailTemplate getTemplate(final MobilityApplicationPeriod mobilityApplicationPeriod, final MobilityProgram mobilityProgram,
                                              final MobilityEmailTemplateType mobilityEmailTemplateType) {
        for (MobilityEmailTemplate template : mobilityApplicationPeriod.getEmailTemplatesSet()) {
            if (template.isFor(mobilityProgram, mobilityEmailTemplateType)) {
                return template;
            }
        }
        return null;
    }
}
