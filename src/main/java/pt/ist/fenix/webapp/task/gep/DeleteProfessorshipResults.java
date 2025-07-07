package pt.ist.fenix.webapp.task.gep;

import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import pt.ist.fenixedu.quc.domain.InquiryResult;

import java.util.Arrays;
import java.util.List;

public class DeleteProfessorshipResults extends WriteCustomTask {

    @Override
    public void runTask() throws Exception {
        List<String> professorShipCodes = Arrays.asList("ist23859_#_AEne-2_#_1_#_2024/2025", "ist25032_#_AEne-2_#_1_#_2024/2025", "ist45793_#_AEne-2_#_1_#_2024/2025", "ist31684_#_Astrob_#_1_#_2024/2025",
                "ist12192_#_Bioc_#_1_#_2024/2025", "ist12351_#_Bioc_#_1_#_2024/2025", "ist12081_#_CProc_#_1_#_2024/2025", "ist12119_#_CProc_#_1_#_2024/2025", "ist12391_#_CProc_#_1_#_2024/2025",
                "ist145829_#_CProc_#_1_#_2024/2025", "ist13160_#_DMole_#_1_#_2024/2025", "ist32907_#_DMole_#_1_#_2024/2025", "ist12544_#_ERA_#_1_#_2024/2025", "ist175072_#_ERA_#_1_#_2024/2025",
                "ist163499_#_LEQ_#_1_#_2024/2025", "ist416959_#_PEQuim_#_1_#_2024/2025", "ist12428_#_PSAva_#_1_#_2024/2025", "ist13181_#_PSAva_#_1_#_2024/2025", "ist90270_#_PSAva_#_1_#_2024/2025",
                "ist12351_#_PTEfl_#_1_#_2024/2025", "ist12534_#_PTEfl_#_1_#_2024/2025", "ist34214_#_PTEfl_#_1_#_2024/2025", "ist11988_#_SIDS_#_1_#_2024/2025", "ist14021_#_SIDS_#_1_#_2024/2025",
                "ist12192_#_SIPro_#_1_#_2024/2025", "ist174706_#_SIPro_#_1_#_2024/2025", "ist416959_#_SIPro_#_1_#_2024/2025", "ist12178_#_SUI_#_1_#_2024/2025", "ist12351_#_SUI_#_1_#_2024/2025",
                "ist12605_#_SUI_#_1_#_2024/2025", "ist145966_#_SUI_#_1_#_2024/2025", "ist12034_#_TIQ_#_1_#_2024/2025", "ist12553_#_TIQ_#_1_#_2024/2025");

        professorShipCodes.stream()
                .map(InquiryResult::getProfessorship)
                .forEach(professorship -> professorship.getInquiryResultsSet()
                        .forEach(InquiryResult::delete));
    }
}
