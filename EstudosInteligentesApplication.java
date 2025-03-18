package com.exemplo.estudosinteligentes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import javax.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import java.time.LocalDate;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class EstudosInteligentesApplication {
    public static void main(String[] args) {
        SpringApplication.run(EstudosInteligentesApplication.class, args);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

@EnableWebSecurity
class SecurityConfig {
    private final BCryptPasswordEncoder passwordEncoder;

    public SecurityConfig(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .httpBasic();
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
            .username("user")
            .password(passwordEncoder.encode("password"))
            .roles("USER")
            .build();
        return new InMemoryUserDetailsManager(user);
    }
}

@Entity
class Estudo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titulo;
    private String descricao;
    private int dificuldade;
    private LocalDate proximaRevisao;
    private String email;
    private String telefone;

    // Getters e Setters
    // ...
}

interface EstudoRepository extends JpaRepository<Estudo, Long> {}

@Service
class EstudoService {
    private final EstudoRepository repository;
    private final JavaMailSender mailSender;
    private final TwilioConfig twilioConfig;

    public EstudoService(EstudoRepository repository, JavaMailSender mailSender, TwilioConfig twilioConfig) {
        this.repository = repository;
        this.mailSender = mailSender;
        this.twilioConfig = twilioConfig;
        Twilio.init(twilioConfig.getAccountSid(), twilioConfig.getAuthToken());
    }

    public List<Estudo> listar() {
        return repository.findAll();
    }

    public Estudo criar(Estudo estudo) {
        estudo.setProximaRevisao(LocalDate.now().plusDays(1));
        return repository.save(estudo);
    }

    public Estudo atualizar(Long id, Estudo estudo) {
        return repository.findById(id).map(e -> {
            e.setTitulo(estudo.getTitulo());
            e.setDescricao(estudo.getDescricao());
            e.setDificuldade(estudo.getDificuldade());
            e.setProximaRevisao(estudo.getProximaRevisao().plusDays(2));
            return repository.save(e);
        }).orElseThrow(() -> new RuntimeException("Estudo não encontrado"));
    }

    public void deletar(Long id) {
        repository.deleteById(id);
    }

    public void enviarNotificacoes() {
        List<Estudo> estudos = repository.findAll();
        for (Estudo estudo : estudos) {
            if (estudo.getProximaRevisao().equals(LocalDate.now())) {
                enviarEmail(estudo);
                enviarWhatsApp(estudo);
            }
        }
    }

    private void enviarEmail(Estudo estudo) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(estudo.getEmail());
        message.setSubject("Lembrete de Revisão");
        message.setText("Está na hora de revisar: " + estudo.getTitulo());
        mailSender.send(message);
    }

    private void enviarWhatsApp(Estudo estudo) {
        if (estudo.getTelefone() != null && !estudo.getTelefone().isEmpty()) {
            Message whatsappMessage = Message.creator(
                new com.twilio.type.PhoneNumber("whatsapp:" + estudo.getTelefone()),
                new com.twilio.type.PhoneNumber("whatsapp:" + twilioConfig.getPhoneNumber()),
                "Está na hora de revisar: " + estudo.getTitulo()
            ).create();
        }
    }
}

@RestController
@RequestMapping("/estudos")
class EstudoController {
    private final EstudoService service;

    public EstudoController(EstudoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Estudo> listar() {
        return service.listar();
    }

    @PostMapping
    public Estudo criar(@RequestBody Estudo estudo) {
        return service.criar(estudo);
    }

    @PutMapping("/{id}")
    public Estudo atualizar(@PathVariable Long id, @RequestBody Estudo estudo) {
        return service.atualizar(id, estudo);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        service.deletar(id);
    }
}

@Configuration
class TwilioConfig {
    @Value("${twilio.account.sid}")
    private String accountSid;
    @Value("${twilio.auth.token}")
    private String authToken;
    @Value("${twilio.phone.number}")
    private String phoneNumber;

    public String getAccountSid() { return accountSid; }
    public String getAuthToken() { return authToken; }
    public String getPhoneNumber() { return phoneNumber; }
}

@Service
class AgendamentoService {
    private final EstudoService estudoService;

    public AgendamentoService(EstudoService estudoService) {
        this.estudoService = estudoService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void enviarNotificacoes() {
        estudoService.enviarNotificacoes();
    }
}