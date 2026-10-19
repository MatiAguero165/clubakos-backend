package com.clubakos.backend.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // =======================================================
    // 1. MÉTODO ORIGINAL (Para correos con adjuntos y HTML)
    // =======================================================
    public void enviarCorreoConAdjunto(String destinatario, String asunto, String mensajeHtml, MultipartFile adjunto) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // El "true" indica que permitimos formato multipart (adjuntos)
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(mensajeHtml, true); // true indica que renderizará HTML

            // Si el administrador subió un archivo, lo adjuntamos al vuelo
            if (adjunto != null && !adjunto.isEmpty()) {
                helper.addAttachment(adjunto.getOriginalFilename(), adjunto);
            }

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error al despachar correo a " + destinatario + ": " + e.getMessage());
        }
    }

    // =======================================================
    // 2. MÉTODO ORIGINAL (Para correos simples de los recordatorios)
    // =======================================================
    public void enviarEmail(String para, String asunto, String mensaje) {
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(para);
            email.setSubject(asunto);
            email.setText(mensaje);
            email.setFrom("administracion@clubakos.com"); // Nombre remitente
            mailSender.send(email);
        } catch (Exception e) {
            System.out.println("Error enviando email simple a: " + para + " - " + e.getMessage());
        }
    }

    // =======================================================
    // 3. NUEVO MÉTODO (Para correos HTML sin adjunto, como confirmación de prueba)
    // =======================================================
    public void enviarCorreoHtml(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // El 'true' indica que el texto es HTML

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error al enviar el correo HTML a " + to + ": " + e.getMessage());
        }
    }
}