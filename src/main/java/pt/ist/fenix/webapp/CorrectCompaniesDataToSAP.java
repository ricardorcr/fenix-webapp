package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.contacts.PartyContactType;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixframework.FenixFramework;

public class CorrectCompaniesDataToSAP extends CustomTask {

    @Override
    public void runTask() throws Exception {

        //ADIST
        Party adist = FenixFramework.getDomainObject("2160369615720");
        PhysicalAddress adistPhysicalAddress = adist.getDefaultPhysicalAddress();
        adistPhysicalAddress.setAddress("Av. Manuel da Maia, 36 R/C Dto");
        adistPhysicalAddress.setArea("Lisboa");
        adistPhysicalAddress.setAreaCode("1000-201");
        adistPhysicalAddress.setAreaOfAreaCode("Lisboa");
        adistPhysicalAddress.setCountryOfResidence(Country.readDefault());
        adistPhysicalAddress.setType(PartyContactType.INSTITUTIONAL);

        adist.setSocialSecurityNumber("PT" + adist.getSocialSecurityNumber());

        //FCT
        Party fct = FenixFramework.getDomainObject("2160369478271");
        PhysicalAddress fctAddress = new PhysicalAddress(fct, PartyContactType.INSTITUTIONAL, true, "Av. D. Carlos I, 126",
                "1249-074", "Lisboa", "Lisboa", null, null, null, Country.readDefault());

    }
}
