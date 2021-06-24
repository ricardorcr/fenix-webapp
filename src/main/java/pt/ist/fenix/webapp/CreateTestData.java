package pt.ist.fenix.webapp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Random;

public class CreateTestData extends CustomTask {

    private static final Locale PT = new Locale("pt", "PT");
    private static final Locale EN = new Locale("en", "GB");

    @Override
    public void runTask() throws Exception {
/*
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .flatMap(admissionProcess -> admissionProcess.getAdmissionProcessTargetSet().stream())
                .flatMap(admissionProcessTarget -> admissionProcessTarget.getApplicationSet().stream())
                .forEach(application -> application.setData(new JsonObject().toString()));
*/
/*
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .forEach(admissionProcess -> setFormData(admissionProcess));
*/

        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .forEach(admissionProcess -> admissionProcess.delete());

        createInternation1stCycle();
        createInternation2ndCycle();
    }

    private void createInternation1stCycle() {
        final LocalizedString title = ls("Candidaturas internacionais ao 1º ciclo", "International applications to 1st cycle");
        final DateTime startDate = new DateTime().minusDays(1);
        final DateTime endDate = startDate.plusMonths(1);
        final String informationURL = "https://tecnico.ulisboa.pt";
        final LocalizedString tags = ls("LEX Bla", "LEX Glurp");

        final String supportEmail = "suport@tecnico.ulisboa.pt";
        final AdmissionProcess admissionProcess = AdmissionProcess.createAdmissionProcess(title, startDate, endDate, informationURL, tags, supportEmail);
        for (int j = 0; j < 25; j++) {
            final Integer slots = j * 5;
            admissionProcess.createAdmissionProcessTarget(ls("Curso" + j, "Target" + j), slots);
        }

        setFormDataInternational1stCycle(admissionProcess);

        final JsonObject config = new JsonObject();
        config.add("description", ls("Emolumentos de candidatura", "Application Fee").json());
        config.addProperty("productCode", "0031");
        config.addProperty("productDescription", "TAXAS DE MATRICULA");
        admissionProcess.getAdmissionProcessCostSet().forEach(cost -> {
            cost.setConfig(config.toString());
        });
        admissionProcess.addAdmissionProcessCost(ls("Emolumento de candidatura", "Application fee"), new BigDecimal(100));
    }

    private void setFormDataInternational1stCycle(final AdmissionProcess admissionProcess) {
        final JsonObject formData = new JsonObject();
        final JsonArray sections = new JsonArray();
        formData.add("sections", sections);

        {
            final JsonObject section = new JsonObject();
            section.add("title", ls("Habilitações", "Qualifications").json());
            section.add("description", ls("Dados sobre as habilitações", "Questions about applicant qualifications").json());
            final JsonArray properties = new JsonArray();
            section.add("properties", properties);

            {
                final JsonObject arrayProperty = new JsonObject();
                properties.add(arrayProperty);
                arrayProperty.addProperty("field", "field11");
                arrayProperty.addProperty("type", "Array");
                final JsonArray arrayProperties = new JsonArray();
                arrayProperty.add("properties", arrayProperties);

                {
                    final JsonObject property = new JsonObject();
                    arrayProperties.add(property);
                    property.addProperty("field", "field11");
                    property.addProperty("type", "String");
                    property.addProperty("required", false);
                    property.add("label", ls("Designação da habilitação", "Qualification designation").json());
                    property.add("tooltip", ls("Texto de ajuda para a habilitação", "Tooltip for qualification").json());
                }
                {
                    final JsonObject property = new JsonObject();
                    arrayProperties.add(property);
                    property.addProperty("field", "field12");
                    property.addProperty("type", "String");
                    property.addProperty("required", false);
                    property.add("label", ls("Estabelecimento de ensino", "Education institution").json());
                    property.add("tooltip", ls("Texto de ajuda para o Estabelecimento", "Tooltip for institution").json());
                }
                {
                    final JsonObject property = new JsonObject();
                    arrayProperties.add(property);
                    property.addProperty("field", "field13");
                    property.addProperty("type", "Date");
                    property.addProperty("required", false);
                    property.add("label", ls("Data de conclusão", "Conclusion date").json());
                    property.add("tooltip", ls("Texto de ajuda para o Ano", "Tooltip for year").json());
                }
                {
                    final JsonObject property = new JsonObject();
                    arrayProperties.add(property);
                    property.addProperty("field", "field14");
                    property.addProperty("type", "Country");
                    property.addProperty("required", false);
                    property.addProperty("url", getCountryURL());
                    property.add("label", ls("País em que obteve a habilitação", "Country in which the qualification was granted").json());
                    property.add("tooltip", ls("Texto de ajuda País de Habilitação", "Tooltip for Degree Country").json());
                }
                {
                    final JsonObject property = new JsonObject();
                    arrayProperties.add(property);
                    property.addProperty("field", "field15");
                    property.addProperty("type", "String");
                    property.addProperty("required", false);
                    property.add("label", ls("Média final do curso/escala", "Degree final grade/scale").json());
                    property.add("tooltip", ls("Texto de ajuda para a média", "Tooltip for grade").json());
                }
            }
            {
                final JsonObject property = new JsonObject();
                properties.add(property);
                property.addProperty("field", "field15");
                property.addProperty("type", "String");
                property.addProperty("required", false);
                property.add("label", ls("Designação da habilitação (não concluída)", "Qualification designation (not concluded)").json());
                property.add("tooltip", ls("Texto de ajuda para a habilitação", "Tooltip for qualification").json());
            }
            {
                final JsonObject property = new JsonObject();
                properties.add(property);
                property.addProperty("field", "field15");
                property.addProperty("type", "String");
                property.addProperty("required", false);
                property.add("label", ls("Estabelecimento de ensino (não concluída)", "Education institution (not concluded)").json());
                property.add("tooltip", ls("Texto de ajuda para o Estabelecimento", "Tooltip for institution").json());
            }
            sections.add(section);
        }

        //TODO Habilitações - última matrícula pertence instituição da UTL/UL? ???

        {
            final JsonObject section = new JsonObject();
            section.add("title", ls("Dados gerais", "General data").json());
            section.add("description", ls("Dados gerais sobre o candidato", "General questions about the applicant").json());
            final JsonArray properties = new JsonArray();
            section.add("properties", properties);

            {
                final JsonObject property = new JsonObject();
                properties.add(property);
                property.addProperty("field", "field21");
                property.addProperty("type", "String");
                property.addProperty("required", false);
                property.add("label", ls("Línguas faladas", "Spoken languages").json());
                property.add("tooltip", ls("Línguas faladas", "Spoken languages").json());
            }
            {
                final JsonObject property = new JsonObject();
                properties.add(property);
                property.addProperty("field", "field22");
                property.addProperty("type", "Boolean");
                property.addProperty("required", false);
                property.add("label", ls("Portador de deficiência?", "Any disabily?").json());
                property.add("labelYes", ls("Sim", "Yes").json());
                property.add("labelNo", ls("Não", "No").json());
                property.add("tooltip", ls("Texto de ajuda para deficiência", "Tooltip for disabled").json());
            }
            {
                final JsonObject property = new JsonObject();
                properties.add(property);
                property.addProperty("field", "field23");
                property.addProperty("type", "String");
                property.addProperty("required", false);
                property.add("label", ls("Se respondeu 'Sim' na pergunta anterior detalhe por favor", "If you answered 'Yes' in the previous question please detail").json());
                property.add("tooltip", ls("Texto de ajuda para deficiência", "Tooltip for disabled").json());
            }
            {
                final JsonObject property = new JsonObject();
                properties.add(property);
                property.addProperty("field", "field24");
                property.addProperty("type", "String");
                property.addProperty("required", false);
                property.add("label", ls("Observações", "Comments").json());
                property.add("tooltip", ls("Texto de ajuda para observações", "Tooltip for comments").json());
            }
            sections.add(section);
        }

        final JsonObject documentsSection = new JsonObject();
        documentsSection.add("title", ls("Documentos", "Documents").json());
        documentsSection.add("description", ls("Documentos necessários para prosseguir com a candidatura", "Necessary documents to proceed with the application").json());
        final JsonArray properties = new JsonArray();
        documentsSection.add("properties", properties);
        {
            final JsonObject property = new JsonObject();
            properties.add(property);
            property.addProperty("field", "PORTUGUESE_LANGUAGE_KNOWLEDGE");
            property.addProperty("type", "File");
            property.addProperty("required", true);
            property.add("label", ls("Declaração de conhecimentos de Língua Portuguesa", "Portuguese language knowledge declaration").json());
            property.add("tooltip", ls("Texto de ajuda para lingua portuguesa", "Tooltip for portuguese language").json());
        }
        {
            final JsonObject property = new JsonObject();
            properties.add(property);
            property.addProperty("field", "DEGREE_CERTIFICATE");
            property.addProperty("type", "File");
            property.addProperty("required", true);
            property.add("label", ls("Certificado final de curso ou equivalente", "Final degree or equivalent certificate").json());
            property.add("tooltip", ls("Texto de ajuda para certificado", "Tooltip for certificate").json());
        }
        {
            final JsonObject property = new JsonObject();
            properties.add(property);
            property.addProperty("field", "PAYMENT_PROOF");
            property.addProperty("type", "File");
            property.addProperty("required", true);
            property.add("label", ls("Comprovativo do pagamento dos emolumentos", "Proof of payment").json());
            property.add("tooltip", ls("Texto de ajuda para o pagamento", "Tooltip for payment").json());
        }
        {
            final JsonObject property = new JsonObject();
            properties.add(property);
            property.addProperty("field", "DOCUMENT_ID");
            property.addProperty("type", "File");
            property.addProperty("required", true);
            property.add("label", ls("Cópia do documento de identificação", "Identification document").json());
            property.add("tooltip", ls("Texto de ajuda para o doc ID", "Tooltip for ID doc").json());
        }
        {
            final JsonObject property = new JsonObject();
            properties.add(property);
            property.addProperty("field", "CURRICULUM");
            property.addProperty("type", "File");
            property.addProperty("required", true);
            property.add("label", ls("Curriculum Vitae", "Curriculum Vitae").json());
            property.add("tooltip", ls("Texto de ajuda para o cv", "Tooltip for cv").json());
        }
        {
            final JsonObject property = new JsonObject();
            properties.add(property);
            property.addProperty("field", "PHOTO");
            property.addProperty("type", "File");
            property.addProperty("required", true);
            property.add("label", ls("Foto", "Photo").json());
            property.add("tooltip", ls("Texto de ajuda para a foto", "Tooltip for photo").json());
        }
        {
            final JsonObject property = new JsonObject();
            properties.add(property);
            property.addProperty("field", "SCALE");
            property.addProperty("type", "File");
            property.addProperty("required", true);
            property.add("label", ls("Documento que especifica a escala de avaliação utilizada", "Grade scale explanation document").json());
            property.add("tooltip", ls("Texto de ajuda para a escala", "Tooltip for scale").json());
        }
        {
            final JsonObject property = new JsonObject();
            properties.add(property);
            property.addProperty("field", "REPORT");
            property.addProperty("type", "File");
            property.addProperty("required", true);
            property.add("label", ls("Relatório ou Obra", "Report or work").json());
            property.add("tooltip", ls("Texto de ajuda para o relatório", "Tooltip for report").json());
        }
        {
            final JsonObject property = new JsonObject();
            properties.add(property);
            property.addProperty("field", "HONOR_DECLARATION");
            property.addProperty("type", "File");
            property.addProperty("required", true);
            property.add("label", ls("Declaração de honra", "Honor declaration").json());
            property.add("tooltip", ls("Texto de ajuda honra", "Tooltip for honor").json());
        }
        sections.add(documentsSection);
        admissionProcess.setFormData(formData.toString());

//        Example types
//        {
//            final JsonObject property = new JsonObject();
//            properties.add(property);
//            property.addProperty("field", "field" + i + "4");
//            property.addProperty("type", "Date");
//            property.addProperty("required", false);
//            property.add("label", ls("Data de conclusão", "Conclusion date").json());
//            property.add("tooltip", ls("Texto de ajuda data conclusão", "Tooltip for conclusion date").json());
//        }
//        {
//            final JsonObject property = new JsonObject();
//            properties.add(property);
//            property.addProperty("field", "field" + i + "5");
//            property.addProperty("type", "Country");
//            property.addProperty("required", false);
//            property.addProperty("url", getCountryURL());
//            property.add("label", ls("País de Habilitação", "Degree Country").json());
//            property.add("tooltip", ls("Texto de ajuda País de Habilitação", "Tooltip for Degree Country").json());
//        }
//        {
//            final JsonObject property = new JsonObject();
//            properties.add(property);
//            property.addProperty("field", "field" + i + "6");
//            property.addProperty("type", "Autocomplete");
//            property.addProperty("required", false);
//            property.addProperty("url", getAutocompleteCountryUrl());
//            property.add("label", ls("País de origem", "Origin country").json());
//            property.add("tooltip", ls("Texto de ajuda País de origem", "Tooltip for Origin country").json());
//        }
    }

    private void createInternation2ndCycle() {
        final LocalizedString title = ls("Candidaturas internacionais ao 2º ciclo", "International applications to 2nd cycle");
        final DateTime startDate = new DateTime().minusDays(1);
        final DateTime endDate = startDate.plusMonths(1);
        final String informationURL = "https://tecnico.ulisboa.pt";
        final LocalizedString tags = ls("LEX Bla", "LEX Glurp");

        final String supportEmail = "suport@tecnico.ulisboa.pt";
        final AdmissionProcess admissionProcess = AdmissionProcess.createAdmissionProcess(title, startDate, endDate, informationURL, tags, supportEmail);
        for (int j = 0; j < 25; j++) {
            final Integer slots = j * 5;
            admissionProcess.createAdmissionProcessTarget(ls("Curso" + j, "Target" + j), slots);
        }

        setFormDataInternational2ndCycle(admissionProcess);

        final JsonObject config = new JsonObject();
        config.add("description", ls("Emolumentos de candidatura", "Application Fee").json());
        config.addProperty("productCode", "0031");
        config.addProperty("productDescription", "TAXAS DE MATRICULA");
        admissionProcess.getAdmissionProcessCostSet().forEach(cost -> {
            cost.setConfig(config.toString());
        });
        admissionProcess.addAdmissionProcessCost(ls("Emolumento de candidatura", "Application fee"), new BigDecimal(100));
    }

    private void setFormDataInternational2ndCycle(final AdmissionProcess admissionProcess) {
        final JsonObject formData = new JsonObject();
        final JsonArray sections = new JsonArray();
        formData.add("sections", sections);

        {
            final JsonObject section = new JsonObject();
            section.add("title", ls("Habilitações", "Qualifications").json());
            section.add("description", ls("Dados sobre as habilitações", "Questions about applicant qualifications").json());
            final JsonArray properties = new JsonArray();
            section.add("properties", properties);

            {
                final JsonObject arrayProperty = new JsonObject();
                properties.add(arrayProperty);
                arrayProperty.addProperty("field", "field11");
                arrayProperty.addProperty("type", "Array");
                final JsonArray arrayProperties = new JsonArray();
                arrayProperty.add("properties", arrayProperties);

                {
                    final JsonObject property = new JsonObject();
                    arrayProperties.add(property);
                    property.addProperty("field", "field11");
                    property.addProperty("type", "String");
                    property.addProperty("required", false);
                    property.add("label", ls("Designação da habilitação", "Qualification designation").json());
                    property.add("tooltip", ls("Texto de ajuda para a habilitação", "Tooltip for qualification").json());
                }
                {
                    final JsonObject property = new JsonObject();
                    arrayProperties.add(property);
                    property.addProperty("field", "field12");
                    property.addProperty("type", "String");
                    property.addProperty("required", false);
                    property.add("label", ls("Estabelecimento de ensino", "Education institution").json());
                    property.add("tooltip", ls("Texto de ajuda para o Estabelecimento", "Tooltip for institution").json());
                }
                {
                    final JsonObject property = new JsonObject();
                    arrayProperties.add(property);
                    property.addProperty("field", "field13");
                    property.addProperty("type", "Date");
                    property.addProperty("required", false);
                    property.add("label", ls("Data de conclusão", "Conclusion date").json());
                    property.add("tooltip", ls("Texto de ajuda para o Ano", "Tooltip for year").json());
                }
                {
                    final JsonObject property = new JsonObject();
                    arrayProperties.add(property);
                    property.addProperty("field", "field14");
                    property.addProperty("type", "Country");
                    property.addProperty("required", false);
                    property.addProperty("url", getCountryURL());
                    property.add("label", ls("País em que obteve a habilitação", "Country in which the qualification was granted").json());
                    property.add("tooltip", ls("Texto de ajuda País de Habilitação", "Tooltip for Degree Country").json());
                }
                {
                    final JsonObject property = new JsonObject();
                    arrayProperties.add(property);
                    property.addProperty("field", "field15");
                    property.addProperty("type", "String");
                    property.addProperty("required", false);
                    property.add("label", ls("Média final do curso/escala", "Degree final grade/scale").json());
                    property.add("tooltip", ls("Texto de ajuda para a média", "Tooltip for grade").json());
                }
            }
            {
                final JsonObject property = new JsonObject();
                properties.add(property);
                property.addProperty("field", "field15");
                property.addProperty("type", "String");
                property.addProperty("required", false);
                property.add("label", ls("Designação da habilitação (não concluída)", "Qualification designation (not concluded)").json());
                property.add("tooltip", ls("Texto de ajuda para a habilitação", "Tooltip for qualification").json());
            }
            {
                final JsonObject property = new JsonObject();
                properties.add(property);
                property.addProperty("field", "field15");
                property.addProperty("type", "String");
                property.addProperty("required", false);
                property.add("label", ls("Estabelecimento de ensino (não concluída)", "Education institution (not concluded)").json());
                property.add("tooltip", ls("Texto de ajuda para o Estabelecimento", "Tooltip for institution").json());
            }
            sections.add(section);
        }

        //TODO Habilitações - última matrícula pertence instituição da UTL/UL? ???

        {
            final JsonObject section = new JsonObject();
            section.add("title", ls("Dados gerais", "General data").json());
            section.add("description", ls("Dados gerais sobre o candidato", "General questions about the applicant").json());
            final JsonArray properties = new JsonArray();
            section.add("properties", properties);

            {
                final JsonObject property = new JsonObject();
                properties.add(property);
                property.addProperty("field", "field21");
                property.addProperty("type", "String");
                property.addProperty("required", false);
                property.add("label", ls("Línguas faladas", "Spoken languages").json());
                property.add("tooltip", ls("Línguas faladas", "Spoken languages").json());
            }
            {
                final JsonObject property = new JsonObject();
                properties.add(property);
                property.addProperty("field", "field22");
                property.addProperty("type", "Boolean");
                property.addProperty("required", false);
                property.add("label", ls("Portador de deficiência?", "Any disabily?").json());
                property.add("labelYes", ls("Sim", "Yes").json());
                property.add("labelNo", ls("Não", "No").json());
                property.add("tooltip", ls("Texto de ajuda para deficiência", "Tooltip for disabled").json());
            }
            {
                final JsonObject property = new JsonObject();
                properties.add(property);
                property.addProperty("field", "field23");
                property.addProperty("type", "String");
                property.addProperty("required", false);
                property.add("label", ls("Se respondeu 'Sim' na pergunta anterior detalhe por favor", "If you answered 'Yes' in the previous question please detail").json());
                property.add("tooltip", ls("Texto de ajuda para deficiência", "Tooltip for disabled").json());
            }
            {
                final JsonObject property = new JsonObject();
                properties.add(property);
                property.addProperty("field", "field24");
                property.addProperty("type", "String");
                property.addProperty("required", false);
                property.add("label", ls("Observações", "Comments").json());
                property.add("tooltip", ls("Texto de ajuda para observações", "Tooltip for comments").json());
            }
            sections.add(section);
        }

        final JsonObject documentsSection = new JsonObject();
        documentsSection.add("title", ls("Documentos", "Documents").json());
        documentsSection.add("description", ls("Documentos necessários para prosseguir com a candidatura", "Necessary documents to proceed with the application").json());
        final JsonArray properties = new JsonArray();
        documentsSection.add("properties", properties);
        documentsSection.add("properties", properties);
        {
            final JsonObject property = new JsonObject();
            properties.add(property);
            property.addProperty("field", "PORTUGUESE_LANGUAGE_KNOWLEDGE");
            property.addProperty("type", "File");
            property.addProperty("required", true);
            property.add("label", ls("Declaração de conhecimentos de Língua Portuguesa", "Portuguese language knowledge declaration").json());
            property.add("tooltip", ls("Texto de ajuda para lingua portuguesa", "Tooltip for portuguese language").json());
        }
        {
            final JsonObject property = new JsonObject();
            properties.add(property);
            property.addProperty("field", "DEGREE_CERTIFICATE");
            property.addProperty("type", "File");
            property.addProperty("required", true);
            property.add("label", ls("Certificado final de curso ou equivalente", "Final degree or equivalent certificate").json());
            property.add("tooltip", ls("Texto de ajuda para certificado", "Tooltip for certificate").json());
        }
        {
            final JsonObject property = new JsonObject();
            properties.add(property);
            property.addProperty("field", "PAYMENT_PROOF");
            property.addProperty("type", "File");
            property.addProperty("required", true);
            property.add("label", ls("Comprovativo do pagamento dos emolumentos", "Proof of payment").json());
            property.add("tooltip", ls("Texto de ajuda para o pagamento", "Tooltip for payment").json());
        }
        {
            final JsonObject property = new JsonObject();
            properties.add(property);
            property.addProperty("field", "DOCUMENT_ID");
            property.addProperty("type", "File");
            property.addProperty("required", true);
            property.add("label", ls("Cópia do documento de identificação", "Identification document").json());
            property.add("tooltip", ls("Texto de ajuda para o doc ID", "Tooltip for ID doc").json());
        }
        {
            final JsonObject property = new JsonObject();
            properties.add(property);
            property.addProperty("field", "CURRICULUM");
            property.addProperty("type", "File");
            property.addProperty("required", true);
            property.add("label", ls("Curriculum Vitae", "Curriculum Vitae").json());
            property.add("tooltip", ls("Texto de ajuda para o cv", "Tooltip for cv").json());
        }
        {
            final JsonObject property = new JsonObject();
            properties.add(property);
            property.addProperty("field", "PHOTO");
            property.addProperty("type", "File");
            property.addProperty("required", true);
            property.add("label", ls("Foto", "Photo").json());
            property.add("tooltip", ls("Texto de ajuda para a foto", "Tooltip for photo").json());
        }
        {
            final JsonObject property = new JsonObject();
            properties.add(property);
            property.addProperty("field", "SCALE");
            property.addProperty("type", "File");
            property.addProperty("required", true);
            property.add("label", ls("Documento que especifica a escala de avaliação utilizada", "Grade scale explanation document").json());
            property.add("tooltip", ls("Texto de ajuda para a escala", "Tooltip for scale").json());
        }
        {
            final JsonObject property = new JsonObject();
            properties.add(property);
            property.addProperty("field", "REPORT");
            property.addProperty("type", "File");
            property.addProperty("required", true);
            property.add("label", ls("Relatório ou Obra", "Report or work").json());
            property.add("tooltip", ls("Texto de ajuda para o relatório", "Tooltip for report").json());
        }
        {
            final JsonObject property = new JsonObject();
            properties.add(property);
            property.addProperty("field", "HONOR_DECLARATION");
            property.addProperty("type", "File");
            property.addProperty("required", true);
            property.add("label", ls("Declaração de honra", "Honor declaration").json());
            property.add("tooltip", ls("Texto de ajuda honra", "Tooltip for honor").json());
        }
        sections.add(documentsSection);
        admissionProcess.setFormData(formData.toString());
    }

    private String getCountryURL() {
        return CoreConfiguration.getConfiguration().applicationUrl() + "/connect/resources/supported-countries/";
    }

    private String getAutocompleteCountryUrl() {
        return CoreConfiguration.getConfiguration().applicationUrl() + "/admissions-server/application/countries/";
    }

    private String getExecutionYearUrl() {
        return CoreConfiguration.getConfiguration().applicationUrl() + "/admissions-server/application/executionYears/";
    }

    private LocalizedString ls(final String pt, final String en) {
        return new LocalizedString(PT, pt).with(EN, en);
    }

}