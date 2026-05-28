package com.ConectaAI.demo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GroqService {

    private final PdfService pdfService;
    private final TimeContextService timeContextService;
    private final ProductCatalogService productCatalogService;

    public GroqService(PdfService pdfService, TimeContextService timeContextService, ProductCatalogService productCatalogService) {
        this.pdfService = pdfService;
        this.timeContextService = timeContextService;
        this.productCatalogService = productCatalogService;
    }

    @Value("${groq.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String callGroq(String message) {

        String url = "https://api.groq.com/openai/v1/chat/completions";

        Map<String, Object> body = new HashMap<>();
        body.put("model", "llama-3.1-8b-instant");

        List<Map<String, String>> messages = new ArrayList<>();

        String contexto = pdfService.readPdf();
        String contextoHorarioAtual = timeContextService.getCurrentTimeContext();
        String catalogoOficial = productCatalogService.getCatalogoFormatadoParaIa();

        messages.add(Map.of(
    "role", "system",
    "content",
        "Você é um atendente virtual do Mercadinho Silva. " +
        "Seu papel é atender clientes de forma educada, simples, comercial e objetiva. " +

        "Responda sempre como um atendente de mercado. " +
        "Não responda como terapeuta, amigo pessoal ou assistente genérico. " +
        "Use frases curtas e naturais para WhatsApp. " +
        "Mantenha um tom profissional, simpático e direto. " +

        "Se o cliente mandar comentários soltos, brincadeiras, reclamações leves ou mensagens sem uma pergunta clara, " +
        "responda de forma breve e traga a conversa de volta para produtos, preços, horário ou atendimento. " +
        "Exemplo: 'Entendi 😅 Posso te ajudar com algum produto, preço ou horário?' " +

        "Não use expressões informais demais como 'e aí', 'de boa' ou similares. " +
        "Não diga que o cliente está perdido. " +
        "Não peça contexto demais. " +

        "NÃO invente produtos, preços, horários, formas de pagamento, entrega ou estoque. " +

        "REGRA SOBRE HORÁRIO: " +
        "Use a data e hora atual informada abaixo junto com o horário de funcionamento descrito nas informações da empresa. " +
        "Se, com base nessas informações, a empresa estiver aberta, diga que está aberta. " +
        "Se estiver fechada, diga que está fechada e informe o horário de funcionamento. " +
        "Se as informações de horário forem insuficientes ou ambíguas, diga que não consegue confirmar com certeza e recomende aguardar um atendente. " +

        "NÃO finalize vendas. " +
        "NÃO confirme pedidos. " +
        "NÃO calcule total de compra. " +
        "Essas partes são controladas pelo sistema. " +

        "Quando o cliente perguntar sobre produto, preço, estoque ou horário, responda somente com base nas informações abaixo. " +
        "Se não tiver a informação, diga que não possui essa informação no momento. " +

        "CATÁLOGO OFICIAL: " +
"Para produtos, preços e disponibilidade, use exclusivamente o catálogo oficial informado abaixo. " +
"Se houver conflito entre o PDF e o catálogo oficial, o catálogo oficial tem prioridade para produtos e preços. " +
        
        "Catálogo oficial de produtos:\n" + catalogoOficial + "\n\n" +
        "Data e hora atual:\n" + contextoHorarioAtual + "\n\n" +
        "Informações da empresa:\n\n" + contexto
));

        messages.add(Map.of(
            "role", "user",
            "content", message
        ));

        body.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request =
            new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
            restTemplate.postForEntity(url, request, Map.class);

        Map choice = (Map) ((List) response.getBody().get("choices")).get(0);
        Map messageMap = (Map) choice.get("message");

        return messageMap.get("content").toString();
    }

}