package com.exemplo.estudosinteligentes;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/estudos")
public class EstudoController {
    private final EstudoRepository repository;
    private final JavaMailSender mailSender;
    private static final String TWILIO_SID = "SEU_TWILIO_SID";
    private static final String TWILIO_AUTH_TOKEN = "SEU_TWILIO_AUTH_TOKEN";
    private static final String TWILIO_PHONE_NUMBER = "SEU_TWILIO_NUMERO";

    public EstudoController(EstudoRepository repository, JavaMailSender mailSender) {
        this.repository = repository;
        this.mailSender = mailSender;
        Twilio.init(TWILIO_SID, TWILIO_AUTH_TOKEN);
    }

    @GetMapping
    public List<Estudo> listar() {
        return repository.findAll();
    }

    @PostMapping
    public Estudo criar(@RequestBody Estudo estudo) {
        estudo.setProximaRevisao(LocalDate.now().plusDays(1));
        return repository.save(estudo);
    }

    @PutMapping("/{id}")
    public Estudo atualizar(@PathVariable Long id, @RequestBody Estudo estudo) {
        return repository.findById(id).map(e -> {
            e.setTitulo(estudo.getTitulo());
            e.setDescricao(estudo.getDescricao());
            e.setDificuldade(estudo.getDificuldade());
            e.setProximaRevisao(estudo.getProximaRevisao().plusDays(2));
            return repository.save(e);
        }).orElseThrow(() -> new RuntimeException("Estudo não encontrado"));
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        repository.deleteById(id);
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void enviarNotificacoes() {
        List<Estudo> estudos = repository.findAll();
        for (Estudo estudo : estudos) {
            if (estudo.getProximaRevisao().equals(LocalDate.now())) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(estudo.getEmail());
                message.setSubject("Lembrete de Revisão");
                message.setText("Está na hora de revisar: " + estudo.getTitulo());
                mailSender.send(message);

                if (estudo.getTelefone() != null && !estudo.getTelefone().isEmpty()) {
                    Message whatsappMessage = Message.creator(
                        new com.twilio.type.PhoneNumber("whatsapp:" + estudo.getTelefone()),
                        new com.twilio.type.PhoneNumber("whatsapp:" + TWILIO_PHONE_NUMBER),
                        "Está na hora de revisar: " + estudo.getTitulo()
                    ).create();
                }
            }
        }
    }
}