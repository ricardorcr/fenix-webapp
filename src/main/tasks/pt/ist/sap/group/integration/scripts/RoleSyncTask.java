package pt.ist.sap.group.integration.scripts;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import com.google.gson.JsonObject;

import pt.ist.sap.client.SapStaff;
import pt.ist.sap.group.integration.domain.ColaboratorSituation;
import pt.ist.sap.group.integration.domain.SapWrapper;

@Task(englishTitle = "Read professional information from SAP into local group.")
public class RoleSyncTask extends CustomTask {

    final LocalDate TODAY = LocalDate.now();

    @Override
    public void runTask() throws Exception {
        if (SapWrapper.institutions.isEmpty()) {
            return;
        }

        final SapWrapper builder = SapWrapper.builder();

        final SapStaff sapStaff = new SapStaff();
        for (final String institution : SapWrapper.institutions) {
            final String institutionCode = SapWrapper.institutionCode.apply(institution);

            final JsonObject params = new JsonObject();
            params.addProperty("institution", institution);

            sapStaff.listPersonProfessionalInformation(params).forEach(e -> {
                final ColaboratorSituation colaboratorSituation = new ColaboratorSituation(e.getAsJsonObject());

                final User user = User.findByUsername(colaboratorSituation.username().toLowerCase().trim());
                if (user == null) {
                    //taskLog("Error: No valid user found for %s%n", colaborator.sapId());
                } else {
                    final String username = user.getUsername();
                    if ((colaboratorSituation.inExercise())
                            && isValidToday(colaboratorSituation.beginDate(), colaboratorSituation.endDate())) {
                        register(builder, institutionCode, "group", colaboratorSituation.categoryTypeName(), username);
                        register(builder, institutionCode, "subGroup", colaboratorSituation.categoryTypeCode(), username);
                        register(builder, institutionCode, "category", colaboratorSituation.categoryName(), username);
                        register(builder, institutionCode, "costCenter", colaboratorSituation.costcenter(), username);
                        register(builder, institutionCode, "campus", colaboratorSituation.campus(), username);
                    }
                }
            });
        }

        builder.complete();
    }

    private boolean isValidToday(final String beginDate, final String endDate) {
        final LocalDate begin =
                beginDate == null || beginDate.isEmpty() ? null : LocalDate.parse(beginDate, DateTimeFormatter.ISO_DATE);
        final LocalDate end = endDate == null || endDate.isEmpty() ? null : LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE);
        return (begin == null || begin.isBefore(TODAY)) && (end == null || end.isAfter(TODAY));
    }

    private void register(final SapWrapper builder, final String institutionCode, final String option, final String value,
            final String username) {
        if (!value.isEmpty()) {
            builder.register(option, institutionCode + " " + value, username);
        }
    }

}