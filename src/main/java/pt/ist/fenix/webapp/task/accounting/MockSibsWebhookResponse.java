package pt.ist.fenix.webapp.task.accounting;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.bouncycastle.util.encoders.Hex;
import org.fenixedu.bennu.SibsPaymentsConfiguration;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import pt.ist.fenixframework.FenixFramework;
import pt.ist.payments.domain.SibsPayment;
import pt.ist.payments.util.Encryptor;
import pt.ist.payments.util.SecretKeyLoader;
import pt.ist.payments.util.Encryptor.Cryptogram;

public class MockSibsWebhookResponse extends CustomTask {

    // the external ID of the SibsPayment you want to create a webhook response for
    private static String sibsPaymentExternalId = "290271069732878";

    // the result code to send with this mock payload
    // check https://sibs.docs.onlinepayments.pt/reference/resultCodes for more info
    private static String resultCode = "000.000.000";

    @Override
    public void runTask() throws Exception {
        SibsPayment payment = FenixFramework.getDomainObject(sibsPaymentExternalId);

        JsonObject payloadObj = new JsonObject();
        payloadObj.add("merchantTransactionId", new JsonPrimitive(payment.getApiId()));
        payloadObj.add("paymentType", new JsonPrimitive(payment.getUsedMultibanco() ? "RC" : "DB"));
        payloadObj.add("paymentBrand", new JsonPrimitive(payment.getMethod().getSibsBrands()));
        payloadObj.add("amount", new JsonPrimitive(payment.getValue().toPlainString()));
        payloadObj.add("currency", new JsonPrimitive(payment.getCurrencyCode()));

        JsonObject resultObj = new JsonObject();
        resultObj.add("code", new JsonPrimitive(resultCode));
        payloadObj.add("result", resultObj);

        JsonObject authenticationObj = new JsonObject();
        authenticationObj.add("entityId", new JsonPrimitive(SibsPaymentsConfiguration.getConfiguration().entity()));
        payloadObj.add("authentication", authenticationObj);

        payloadObj.add("timestamp", new JsonPrimitive(DateTime.now().toString("yyyy-MM-dd HH:mm:ssZ")));

        JsonObject mockNotificationObj = new JsonObject();
        mockNotificationObj.add("type", new JsonPrimitive("PAYMENT"));
        mockNotificationObj.add("payload", payloadObj);
        taskLog("Mock Sibs Webhook Notification: %s\n", mockNotificationObj.toString());

        Cryptogram cryptogram = encrypt(mockNotificationObj.toString());
        String iv = Hex.toHexString(cryptogram.getIv());
        String ciphertext = Hex.toHexString(cryptogram.getCiphertext());
        String authTag = Hex.toHexString(cryptogram.getAuthTag());
        taskLog("curl -X POST -H 'Content-Type: text/plain' -H 'X-Initialization-Vector:%s' -H 'X-Authentication-Tag:%s' -d '%s' %s\n",
                iv, authTag, ciphertext,
                CoreConfiguration.getConfiguration().applicationUrl() + "/payments/v1/api/sibshook");
    }

    private static Cryptogram encrypt(String input) throws Exception {
        SecretKey secretKey = SecretKeyLoader.getInstance().getSibsWebhookSecretKey();

        byte[] plaintext = input.getBytes(StandardCharsets.UTF_8);
        return Encryptor.encrypt(plaintext, secretKey);
    }
}