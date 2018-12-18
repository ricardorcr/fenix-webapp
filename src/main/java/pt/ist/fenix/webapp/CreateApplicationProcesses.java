package pt.ist.fenix.webapp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.util.LocaleUtils;
import org.fenixedu.admissions.domain.AdmissionProcessCost;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;

import java.math.BigDecimal;

public class CreateApplicationProcesses extends CustomTask {

    @Override
    public void runTask() throws Exception {
//        LocalizedString title = new LocalizedString(LocaleUtils.PT, "Maiores de 23").with(LocaleUtils.EN, "Above 23");
//        DateTime startDate = new DateTime(2020, 12, 01, 00, 01);
//        DateTime endDate = new DateTime(2020, 12, 31, 23, 59);
//        String url = "http://localhost:8080";
//        LocalizedString tags = new LocalizedString(LocaleUtils.PT, "curso maiores 23").with(LocaleUtils.EN, "degree above 23");
//        AdmissionProcess ap = AdmissionProcess.createAdmissionProcess(title, startDate, endDate, url, tags, "support@admissions.pt", 20);
//        ap.setFormData(getProcessData().toString());
//        addCosts(ap);
//        addTargets(ap);
//
//        LocalizedString title1 = new LocalizedString(LocaleUtils.PT, "Internacionais").with(LocaleUtils.EN, "Internationals");
//        DateTime startDate1 = new DateTime(2020, 12, 01, 00, 01);
//        DateTime endDate1 = new DateTime(2021, 1, 31, 23, 59);
//        String url1 = "http://localhost:8080";
//        LocalizedString tags1 = new LocalizedString(LocaleUtils.PT, "internacionais 2ciclo").with(LocaleUtils.EN, "internationals 2ndcycle");
//        AdmissionProcess ap1 = AdmissionProcess.createAdmissionProcess(title1, startDate1, endDate1, url1, tags1, "support@admissions.pt", 20);
//        ap1.setFormData(getProcessData().toString());
//        addCosts(ap1);
//        addTargets(ap1);
//
//        getProcessData();
    }

//    private void addTargets(AdmissionProcess ap) {
//        Degree.readBolonhaDegrees().forEach(
//            d -> new AdmissionProcessTarget(d.getNameI18N(), ap)
//        );
//    }
//
//    private void addCosts(AdmissionProcess ap) {
//        LocalizedString description = new LocalizedString(LocaleUtils.PT, "Aluno da UL").with(LocaleUtils.EN, "UL student");
//        AdmissionProcessCost apc = new AdmissionProcessCost(description, new BigDecimal("50"));
//        ap.addAdmissionProcessCost(apc);
//
//        description = new LocalizedString(LocaleUtils.PT, "Aluno fora da UL").with(LocaleUtils.EN, "UL outside student");
//        apc = new AdmissionProcessCost(description, new BigDecimal("100"));
//        ap.addAdmissionProcessCost(apc);
//    }
//
//    private JsonObject getProcessData() {
//        JsonObject main = new JsonObject();
//        JsonObject precedentDegree = new JsonObject();
//        precedentDegree.addProperty("title", new LocalizedString(LocaleUtils.PT, "Curso anterior").with(LocaleUtils.EN, "Precedent degree").toString());
//        precedentDegree.addProperty("description", new LocalizedString(LocaleUtils.PT, "Curso anterior realizado").with(LocaleUtils.EN, "Precedent degree enrolled").toString());
//        JsonArray properties = new JsonArray();
//
//        JsonObject schoolOfOrigin = new JsonObject();
//        schoolOfOrigin.addProperty("label", new LocalizedString(LocaleUtils.PT, "Escola de origem").with(LocaleUtils.EN, "Origin school").toString());
//        schoolOfOrigin.addProperty("description", new LocalizedString(LocaleUtils.PT, "Escola de origem").with(LocaleUtils.EN, "Origin school").toString());
//        schoolOfOrigin.addProperty("tooltip", new LocalizedString(LocaleUtils.PT, "Escola de origem").with(LocaleUtils.EN, "Origin school").toString());
//        schoolOfOrigin.addProperty("type", "string");
//        schoolOfOrigin.addProperty("required", "true");
//        properties.add(schoolOfOrigin);
//
//        JsonObject conclusionGrade = new JsonObject();
//        conclusionGrade.addProperty("label", new LocalizedString(LocaleUtils.PT, "Nota de conclusão").with(LocaleUtils.EN, "Conclusion grade").toString());
//        conclusionGrade.addProperty("description", new LocalizedString(LocaleUtils.PT, "Nota de conclusão").with(LocaleUtils.EN, "Conclusion grade").toString());
//        conclusionGrade.addProperty("tooltip", new LocalizedString(LocaleUtils.PT, "Nota de conclusão").with(LocaleUtils.EN, "Conclusion grade").toString());
//        conclusionGrade.addProperty("type", "string");
//        conclusionGrade.addProperty("required", "true");
//        properties.add(conclusionGrade);
//
//        precedentDegree.add("properties", properties);
//        main.add("precedentDegree", precedentDegree);
//        main.add("qualifications", null);
//        taskLog(main.toString());
//        return main;
//    }
}
