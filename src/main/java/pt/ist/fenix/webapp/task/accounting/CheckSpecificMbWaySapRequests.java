package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.payments.domain.SibsPaymentSystem;

import java.util.Arrays;
import java.util.List;

public class CheckSpecificMbWaySapRequests extends ReadCustomTask {

    private Money total = new Money(0);
    @Override
    public void runTask() throws Exception {
        final List<String> mbWays = Arrays.asList("1b2ec1f272f83b65ade01e110", "c151fd2ed79ff2aa463f80b62", "25f8bf736ff7c9ff26012d29a", "2523d01d5057df885182dac42", "d8b48b3fd9953f34e754924ba",
                "b042b753b201b726befe2ec4c", "5681f3f9bda53becd8b2eec4a", "1ff79257cc50ae91a55118390", "542f53cfbeac2a0e51f70703b", "69b8459180172f4deebce2c68", "39df84b8888a8c10963ebc141",
                "88d3ab334bea708263cfc109f", "211733509175f53b2d677f442", "f84173bbf095f071062c98dcf", "c8f6921c67968ebc487f39309", "79791b4671f3c2d50d3770dc0", "9577f0495c18cfc7d863d459f",
                "0ac9ccf77523c863ac740b20d", "7954c0991feb1413577e18a30", "0b3f7a3e2e572832b57153809", "1765c91adf793a23ac1e22848", "c925bc7eb7cdf89e6f65ce05c", "3bd1ee746118d3a31bf27281a",
                "7205f485392ae095c05cda1a8", "9c9a69f1785435423ec68e523", "10aff485a3ac5421cf7783d2a", "f01064e70ed0ecc209520a14b", "ebaf64b345391284a62e8d3a0", "bec7a2b64879059e53fe87de7",
                "04ad1f50236c5b3e253701f1e", "daa3ee9d660e547e13f0780fd", "9b27d84e6d4de11399be1527e", "24c80f4844306944730ac11c5", "0d192aae8950c641f2d19c895", "272366abac860e5f8222eb5e7",
                "eefbcce85c39d8b7c63d4594c", "12fa34a97d39585c3abd7e87f", "0bb32c5a7352c0b60846d000e", "abe1dcd327d11de83ab366b74", "b4e050ceb9848b38c095ce1c2", "5225be7a075116b40d10afbd8",
                "982786a2698afc3c283161ed1", "53d5a398f597340dc2c887045", "84f9db30689efe9c559c09d66", "bb41cba46c0562d0680b508c7", "df97a081beab13a6276a6f7a5", "97cb793e834657bf0ef1a8ecc",
                "c2d7655d429b8e030e85296a7", "2b08c6ec721f226e1e6ab402f", "a2cde6c37d51888948ed26863", "bb73e92e304523d2265575adc", "60879bf99475b2549749dfb5b", "62b1717f8003d7e0ecf364c23",
                "0dc87dcc9a39525c86ecc546d", "e2f87bccbc390b63c0b9c268b", "dda0da8ab37328f13d4cc301c", "2ff81964f2b50fdef94cd493d", "44dc194e6ae9368671ddbda7f", "9c5d1f3f8b718f9135ce87685",
                "1352db3e65fa2befd6b014607", "1976823c8ff752f008647697c", "c38cb8759ef4c4932c934619b", "c290e2b9a72f15120f0406e22", "7ee3b4c6379399b1561a94932", "dcaf1c5d29f62e739cda531fe");

        final Spreadsheet spreadsheet = new Spreadsheet("MbWays");
        SibsPaymentSystem.getInstance().getSibsPaymentSet().stream()
                .filter(sibsPayment -> mbWays.contains(sibsPayment.getMerchantId()))
                .forEach(sibsPayment -> {
                    if (sibsPayment.getAccountingTransaction().getSapRequestSet().isEmpty()) {
                        taskLog("%s\t%s%n", sibsPayment.getEvent(), sibsPayment.getAccountingTransaction().getExternalId());
                    }
                    sibsPayment.getAccountingTransaction().getSapRequestSet().stream()
                            .filter(sr -> sr.getRequestType().equals(SapRequestType.PAYMENT) ||
                                    sr.getRequestType().equals(SapRequestType.PAYMENT_INTEREST) ||
                                    sr.getRequestType().equals(SapRequestType.ADVANCEMENT))
                            .forEach(sr -> report(sr, spreadsheet));
                    total = total.add(sibsPayment.getAccountingTransaction().getOriginalAmount());
                });

        taskLog("Total: %s", total.toString());
        output("mbways.xlsx", spreadsheet.exportToXLSXSheet());
    }

    private void report(final SapRequest sr, final Spreadsheet spreadsheet) {
        Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("Evento", sr.getEvent().getExternalId());
        row.setCell("Doc Number", sr.getDocumentNumber());
        row.setCell("Data Valor", sr.getRequestAsJson().get("paymentDocument").getAsJsonObject().get("sibsDate").getAsString());
        row.setCell("Tipo", sr.getRequestType().toString());
        row.setCell("Valor", sr.getValue()!= null ? sr.getValue().toString() : "0");
        row.setCell("Adiantamento", sr.getAdvancementRequest() != null ? sr.getAdvancement().toString() : "0");
        row.setCell("Enviado", String.valueOf(sr.getSent()));
        row.setCell("Integrado", String.valueOf(sr.getIntegrated()));
        row.setCell("Data envio", sr.getWhenSent() != null ? sr.getWhenSent().toString("dd/MM/yyyy HH:mm:ss") : "");
    }
}
