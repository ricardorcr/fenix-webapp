package pt.ist.fenix.webapp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;

import pt.ist.fenixedu.domain.SapRoot;

public class CheckDifferenceBetweenInitialSAPAndFenixDebt extends ReadCustomTask {

	@Override
	public void runTask() throws Exception {
		final Spreadsheet spreadsheet = new Spreadsheet("Diferenças sapinit-opendebts2017");
		
		final Map<String, Money> eventDebtInitialization = new HashMap<>();
		List<String> docsToExclude = Arrays.asList("ND10159","ND1109","ND13138","ND6471");
		SapRoot.getInstance().getSapRequestSet().stream()
        	.filter(sr -> !sr.isInitialization())
        	.filter(sr -> sr.getDocumentDate().getYear() == 2017 && sr.getDocumentDate().getMonthOfYear() == 12)
        	.filter(sr -> sr.getOriginalRequest() == null && sr.getAnulledRequest() == null)
        	.filter(sr -> !docsToExclude.contains(sr.getDocumentNumber()))
        	.forEach(sr -> {
        		eventDebtInitialization.merge(sr.getEvent().getExternalId(), sr.getValue(), Money::add);
        	});
		
		Money totalInit = eventDebtInitialization.values().stream().reduce(Money.ZERO, Money::add);
		
		try {
			final List<String> amountDue = Files.readAllLines(
					new File("/afs/ist.utl.pt/ciist/fenix/fenix015/ist/amountDue_2017.txt").toPath());
				
			Money debtBiggerThanInit = Money.ZERO;
			Money initBiggerThanDebt = Money.ZERO;
			Money debt2017NotReported = Money.ZERO;
			Money totalOpenDebts = Money.ZERO;
			for (String line : amountDue) {
				String[] split = line.split("\t");
				Money initMoney = eventDebtInitialization.get(split[0]);
				Money dueMoney = new Money(split[1]);
				totalOpenDebts = totalOpenDebts.add(dueMoney);
				if (initMoney == null) {
					debt2017NotReported = debt2017NotReported.add(dueMoney);
					addLine(spreadsheet, split[0], Money.ZERO, dueMoney, "Dívida comunicada com data posterior a 31-12-2017");
//					taskLog("Evento com dívida comunicada depois - valor: %s\t%s%n", dueMoney, split[0]);
				} else if (!initMoney.equals(dueMoney)) {
					Money difference = Money.ZERO;
					if (initMoney.greaterThan(dueMoney)) {
						difference = initMoney.subtract(dueMoney);
						addLine(spreadsheet, split[0], initMoney, dueMoney, "Dívida diminuiu depois do envio");
//						taskLog("Evento com dívida menor que init: initMoney: %s\tdueMoney: %s\tdiferença: %s\t%s%n", initMoney, dueMoney,	difference, split[0]);
						initBiggerThanDebt = initBiggerThanDebt.add(difference);
					} else {
						difference = dueMoney.subtract(initMoney);
						addLine(spreadsheet, split[0], initMoney, dueMoney, "Dívida aumentou depois do envio");
//						taskLog("Evento com dívida Maior que init: initMoney: %s\tdueMoney: %s\tdiferença: %s\t%s%n", initMoney, dueMoney,	difference, split[0]);
						debtBiggerThanInit = debtBiggerThanInit.add(difference);
					}				
				}
			}
			
			Money[] noDebtButInInit = new Money[] {Money.ZERO};
			eventDebtInitialization.keySet()
				.forEach(eventID -> {
					String id = null;	
					for (String line : amountDue) {
						String[] split = line.split("\t");
						if (split[0].equals(eventID)) {
							id = eventID;
							break;
						}
					}
					if (id == null) {
						noDebtButInInit[0] = noDebtButInInit[0].add(eventDebtInitialization.get(eventID));
						addLine(spreadsheet, eventID, eventDebtInitialization.get(eventID), Money.ZERO, "Foi enviada dívida mas não estava correcta");
//						taskLog("Este evento consta no init e não consta no openDebts: %s%n", eventID);
					}
				});
			
			taskLog("%nTotal openDebts2017: %s%n", totalOpenDebts);
			taskLog("Total init: %s%n", totalInit);
			taskLog("Valor no init mas não no openDebts: %s%n", noDebtButInInit[0]);
			taskLog("Dívida de 2017 não comunicada a 31-12: %s%n", debt2017NotReported);
			taskLog("Diferença dívida maior que init: %s%n", debtBiggerThanInit);
			taskLog("Diferença dívida menor que init: %s%n", initBiggerThanDebt);
			
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        spreadsheet.exportToXLSSheet(baos);
	        output("Diferencas_sapinit-opendebts2017.xls", baos.toByteArray());
		
		} catch (IOException e) {
			throw new Error("Erro a ler o ficheiro.");
		}
	}

	private void addLine(final Spreadsheet spreadsheet, String eventID, Money sapInit, Money debtMoney, String comment) {
		Row row = spreadsheet.addRow();
		row.setCell("Evento", eventID);
		row.setCell("Dívida sapInit", sapInit.getAmount());
		row.setCell("Dívida openDebt", debtMoney.getAmount());
		row.setCell("Diferença", debtMoney.subtract(sapInit).getAmount());
		row.setCell("Comentário", comment);
		row.setCell("Evento Fénix", "https://fenix.tecnico.ulisboa.pt/sap-invoice-viewer/" + eventID);
	}
}
