package com.exemplo.estudosinteligentes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.context.annotation.Configuration;
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
import com.sendgrid.*;
import java.io.IOException;
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
}

interface EstudoRepository extends JpaRepository<Estudo, Long> {}

@Service
class SendGridService {

    private static final String API_KEY = "YOUR_SENDGRID_API_KEY";

    public void enviarEmail(String destinatario, String assunto, String corpo) {
        Email from = new Email("seuemail@dominio.com");
        String subject = assunto;
        Email to = new Email(destinatario);
        Content content = new Content("text/plain", corpo);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(API_KEY);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            System.out.println(response.getStatusCode());
            System.out.println(response.getBody());
            System.out.println(response.getHeaders());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

@Service
class EstudoService {
    private final EstudoRepository repository;
    private final SendGridService sendGridService;

    public EstudoService(EstudoRepository repository, SendGridService sendGridService) {
        this.repository = repository;
        this.sendGridService = sendGridService;
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
            }
        }
    }

    private void enviarEmail(Estudo estudo) {
        sendGridService.enviarEmail(estudo.getEmail(), "Lembrete de Revisão", "Está na hora de revisar: " + estudo.getTitulo());
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
