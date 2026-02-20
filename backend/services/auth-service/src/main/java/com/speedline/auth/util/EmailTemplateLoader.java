package com.speedline.auth.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Utilitaire pour charger et formater les templates d'email HTML
 */
@Component
@Slf4j
public class EmailTemplateLoader {

    /**
     * Charge un template HTML depuis le dossier resources/templates
     * 
     * @param templateName Nom du fichier template (sans le chemin)
     * @return Contenu du template HTML
     * @throws IOException Si le template n'est pas trouvé
     */
    public String loadTemplate(String templateName) throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/" + templateName);
        
        if (!resource.exists()) {
            log.error("Template not found: {}", templateName);
            throw new IOException("Email template not found: " + templateName);
        }
        
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * Remplace les placeholders dans le template par les valeurs fournies
     * Les placeholders doivent être au format {{key}}
     * 
     * @param template Template HTML contenant les placeholders
     * @param variables Map des variables à remplacer (key -> value)
     * @return Template avec les valeurs remplacées
     */
    public String formatTemplate(String template, Map<String, String> variables) {
        String result = template;
        
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            result = result.replace(placeholder, entry.getValue());
        }
        
        return result;
    }

    /**
     * Charge et formate un template en une seule opération
     * 
     * @param templateName Nom du fichier template
     * @param variables Variables à remplacer dans le template
     * @return Template HTML formaté
     * @throws IOException Si le template n'est pas trouvé
     */
    public String loadAndFormat(String templateName, Map<String, String> variables) throws IOException {
        String template = loadTemplate(templateName);
        return formatTemplate(template, variables);
    }
}
