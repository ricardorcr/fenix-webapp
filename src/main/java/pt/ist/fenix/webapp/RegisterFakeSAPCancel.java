package pt.ist.fenix.webapp;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

public class RegisterFakeSAPCancel extends CustomTask {

	@Override
	public void runTask() throws Exception {
		Arrays.asList("ND10159","ND1109","ND13138","ND6471","NP351295","NP352603","NP355442","NP355384")
			.forEach(docNumber -> createFakeCancel(docNumber));		
	}

	private void createFakeCancel(String documentNumber) {
		SapRequest original = getSapRequest(documentNumber);
		SapRequest fakeCancel = new SapRequest(original.getEvent(), original.getClientId(), original.getValue(), original.getDocumentNumber(),
				original.getRequestType(), original.getAdvancement(), original.getRequestAsJson());
		fakeCancel.setSent(true);
		fakeCancel.setIntegrated(true);
		fakeCancel.setIgnore(true);
		fakeCancel.setWhenSent(new DateTime());
		fakeCancel.setPayment(original.getPayment());
		fakeCancel.setOriginalRequest(original);
		original.setAnulledRequest(fakeCancel);
	}

	private SapRequest getSapRequest(String documentNumber) {
		List<SapRequest> result = SapRoot.getInstance().getSapRequestSet().stream()
			.filter(sr -> !sr.isInitialization())
			.filter(sr -> sr.getRequestType() == SapRequestType.INVOICE || sr.getRequestType() == SapRequestType.PAYMENT)
			.filter(sr -> sr.getDocumentNumber().equals(documentNumber))
			.collect(Collectors.toList());
		if (result.size() > 1) {
			throw new Error("Mais que um resultado para: " + documentNumber);
		}
		return result.get(0);
	}
}
