package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;

import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

public class Check2019NAs extends ReadCustomTask {

	@Override
	public void runTask() throws Exception {
		long count = SapRoot.getInstance().getSapRequestSet().stream()
			.filter(sr -> !sr.isInitialization())
			.filter(sr -> sr.getRequestType().equals(SapRequestType.CREDIT))
			.filter(sr -> sr.getDocumentDate().getYear() == 2019)
			.filter(sr -> sr.getIntegrated())
			.count();
//			.forEach(sr -> taskLog("%s %s\n", sr.getDocumentNumber(), sr.getEvent().getExternalId()));
		taskLog("" + count);
	}
}
