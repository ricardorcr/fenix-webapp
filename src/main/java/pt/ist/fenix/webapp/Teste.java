package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRoot;

import java.util.HashMap;
import java.util.Map;

public class Teste extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Map<String, String> map = new HashMap<>();

        map.put("5280", "Engenharia e Gestão Industrial");
        map.put("PA67", "Estudos em Saúde Planetária (associação)");
        map.put("5107", "Bioengenharia");
        map.put("5111", "Biotecnologia");
        map.put("5129", "Engenharia Aeroespacial");
        map.put("5130", "Engenharia Civil");
        map.put("5131", "Engenharia de Materiais");
        map.put("5133", "Engenharia do Ambiente");
        map.put("5134", "Engenharia do Território");
        map.put("5136", "Engenharia e Gestão");
        map.put("5138", "Engenharia Electrotécnica e de Computadores");
        map.put("5139", "Engenharia Física Tecnológica");
        map.put("5142", "Engenharia Informática e de Computadores");
        map.put("5143", "Engenharia Mecânica");
        map.put("5144", "Engenharia Naval");
        map.put("5146", "Engenharia Química");
        map.put("5154", "Física");
        map.put("5164", "Georrecursos");
        map.put("5179", "Líderes para Indústrias Tecnológicas");
        map.put("5229", "Química");
        map.put("5271", "Sistemas Sustentáveis de Energia");
        map.put("5296", "Transportes");
        map.put("5391", "Engenharia Computacional");
        map.put("5579", "Estatística e Processos Estocásticos");
        map.put("5581", "Mudança Tecnológica e Empreendedorismo");
        map.put("5583", "Segurança de Informação");
        map.put("5599", "Engenharia e Políticas Públicas");
        map.put("5704", "Restauro e Gestão Fluviais");
        map.put("5731", "Alterações Climáticas e Políticas de Desenvolvimento Sustentável");
        map.put("5740", "Media Digitais");
        map.put("5756", "Engenharia da Refinação, Petroquímica e Química");
        map.put("5799", "Sistemas de Transportes");
        map.put("5911", "Engenharia de Petróleos");
        map.put("5922", "Biotecnologia e Biociências");
        map.put("5943", "Engenharia Naval e Oceânica");
        map.put("5971", "Engenharia do Território e Planeamento Territorial");
        map.put("9337", "Arquitetura");
        map.put("9342", "Matemática");
        map.put("9446", "Engenharia Biomédica");
        map.put("PA05", "Materiais e Processamento Avançados (associação)");
        map.put("PA35", "Filosofia da Ciência, Tecnologia, Arte e Sociedade (associação)");
        map.put("8458", "Estudos Gerais");
        map.put("9089", "Engenharia Civil");
        map.put("9096", "Engenharia de Materiais");
        map.put("9099", "Engenharia do Ambiente");
        map.put("9121", "Engenharia Informática e de Computadores");
        map.put("9123", "Engenharia Mecânica");
        map.put("9125", "Engenharia Química");
        map.put("9345", "Matemática Aplicada e Computação");
        map.put("9455", "Engenharia Biomédica");
        map.put("9474", "Engenharia Biológica");
        map.put("9911", "Engenharia e Arquitetura Naval");
        map.put("9913", "Engenharia Geológica e de Minas");
        map.put("L162", "Engenharia Naval e Oceânica");
        map.put("L203", "Engenharia Eletrotécnica e de Computadores (ULisboa e SHU)");
        map.put("L209", "Engenharia Eletrotécnica e de Computadores");
        map.put("L212", "Engenharia Civil (ULisboa e SHU)");
        map.put("L219", "Ciências Militares Aeronáuticas, especialidade de Engenharia");
        map.put("L221", "Engenharia Aeroespacial");
        map.put("L222", "Engenharia Mecânica Militar");
        map.put("L228", "Engenharia Militar");
        map.put("L233", "Engenharia Física Tecnológica");
        map.put("L238", "Engenharia Eletrotécnica Militar");
        map.put("L239", "Engenharia de Minas e Recursos Energéticos");
        map.put("L250", "Engenharia do Ambiente (ULisboa e SHU)");
        map.put("L289", "Ciências Militares Aeronáuticas, especialidade de Piloto Aviador");
        map.put("L343", "Engenharia Geral (GENI)");
        map.put("8506", "Ciências de Engenharia - Engenharia Civil");
        map.put("8508", "Ciências de Engenharia - Engenharia do Ambiente");
        map.put("9581", "Ciências de Engenharia - Engenharia Aeroespacial");
        map.put("9582", "Ciências de Engenharia - Engenharia Biológica");
        map.put("9583", "Ciências de Engenharia - Engenharia Biomédica");
        map.put("9584", "Ciências de Engenharia - Engenharia Electrotécnica e de Computadores");
        map.put("9585", "Ciências de Engenharia - Engenharia Física Tecnológica");
        map.put("9586", "Ciências de Engenharia - Engenharia Mecânica");
        map.put("9587", "Ciências de Engenharia - Engenharia Química");
        map.put("9614", "Estudos de Arquitetura");
        map.put("H011", "Ciências de Engenharia - Engenharia de Materiais");
        map.put("MC64", "Aeronáutica Militar, especialidade de Engenharia Aeronáutica");
        map.put("MC33", "Aeronáutica Militar, especialidade de Engenharia de Aeródromos");
        map.put("MC34", "Aeronáutica Militar, especialidade de Engenharia Eletrotécnica");
        map.put("6712", "Bioengenharia e Nanossistemas");
        map.put("MC89", "Bioengenharia em Medicina Regenerativa e de Precisão");
        map.put("9399", "Biotecnologia");
        map.put("MD53", "Ciência e Engenharia Moleculares");
        map.put("MC03", "Ciências e Tecnologias para o Património Cultural");
        map.put("M446", "Construção e Reabilitação");
        map.put("MB96", "Engenharia Aeroespacial");
        map.put("MB81", "Engenharia Biológica");
        map.put("9568", "Engenharia Biomédica");
        map.put("9569", "Engenharia Civil");
        map.put("MB67", "Engenharia Civil (ULisboa e SHU)");
        map.put("M440", "Engenharia de Estruturas");
        map.put("M593", "Engenharia de Infraestruturas de Transporte");
        map.put("9415", "Engenharia de Materiais");
        map.put("M592", "Engenharia de Petróleos");
        map.put("M901", "Engenharia de Sistemas de Transporte");
        map.put("9417", "Engenharia do Ambiente");
        map.put("MD01", "Engenharia do Ambiente (ULisboa e SHU)");
        map.put("9418", "Engenharia do Território");
        map.put("9419", "Engenharia e Arquitectura Naval");
        map.put("MA58", "Engenharia e Ciência de Dados");
        map.put("M088", "Engenharia e Gestão da Água");
        map.put("M661", "Engenharia e Gestão da Energia");
        map.put("MA85", "Engenharia e Gestão da Inovação e Empreendedorismo");
        map.put("MB40", "Engenharia Eletrotécnica e de Computadores");
        map.put("MC60", "Engenharia Eletrotécnica Militar");
        map.put("MD58", "Engenharia em Recursos Energéticos");
        map.put("6731", "Engenharia Farmacêutica");
        map.put("MC40", "Engenharia Física Tecnológica");
        map.put("9426", "Engenharia Geológica e de Minas");
        map.put("9427", "Engenharia Informática e de Computadores");
        map.put("6361", "Engenharia Mecânica");
        map.put("MC14", "Engenharia Mecânica Militar");
        map.put("MC42", "Engenharia Militar");
        map.put("M986", "Engenharia Naval e Oceânica");
        map.put("6362", "Engenharia Química");
        map.put("6468", "Física Médica");
        map.put("M682", "Informação e Sistemas Empresariais");
        map.put("MC84", "Matemática Aplicada e Computação");
        map.put("9437", "Matemática e Aplicações");
        map.put("9317", "Microbiologia");
        map.put("M982", "Ordenamento do Território e Urbanismo");
        map.put("M441", "Planeamento e Operação de Transportes");
        map.put("M940", "Proteção e Segurança Radiológica");
        map.put("9442", "Química");
        map.put("MD98", "Reuso de Edifícios Modernos");
        map.put("M754", "Segurança de Informação e Direito no Ciberespaço");
        map.put("6869", "Sistemas Complexos de Infra- Estruturas de Transportes");
        map.put("9658", "Sistemas de Informação Geográfica");
        map.put("MC37", "Sistemas de Transportes");
        map.put("M645", "Tecnologias Biomédicas");
        map.put("M156", "Urbanismo e Ordenamento do Território");
        map.put("9257", "Arquitetura");
        map.put("9357", "Engenharia Aeroespacial");
        map.put("9358", "Engenharia Biológica");
        map.put("9359", "Engenharia Biomédica");
        map.put("9360", "Engenharia Civil");
        map.put("9363", "Engenharia de Materiais");
        map.put("9367", "Engenharia Electrotécnica e de Computadores");
        map.put("9369", "Engenharia Mecânica");
        map.put("9458", "Engenharia Física Tecnológica");
        map.put("9461", "Engenharia Química");
        map.put("9508", "Engenharia do Ambiente");

        map.forEach((key, value) -> {
//            boolean anyMatch = Degree.readNotEmptyDegrees().stream()
//                    .anyMatch(degree -> key.equals(degree.getMinistryCode()));

            boolean anyMatch = Bennu.getInstance().getDegreeDesignationsSet().stream()
                    .anyMatch(dd -> key.equals(dd.getCode()));
            if (!anyMatch) {
                taskLog("%s\t%s%n", key, value);
            }
        });
    }
}
