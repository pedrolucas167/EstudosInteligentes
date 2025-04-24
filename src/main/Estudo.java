package com.exemplo.estudosinteligentes;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Representa um conteúdo de estudo com informações relevantes como título, descrição,
 * nível de dificuldade, data da próxima revisão, e dados de contato.
 */
@Entity
public class Estudo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O título é obrigatório")
    private String titulo;

    @NotBlank(message = "A descrição é obrigatória")
    @Column(length = 2000)
    private String descricao;

    @Min(value = 1, message = "Dificuldade mínima é 1")
    @Max(value = 5, message = "Dificuldade máxima é 5")
    private int dificuldade;

    @FutureOrPresent(message = "A próxima revisão deve ser hoje ou no futuro")
    private LocalDate proximaRevisao;

    @Email(message = "Email inválido")
    private String email;

    @Pattern(regexp = "\\d{10,15}", message = "Telefone deve conter apenas dígitos, entre 10 e 15 caracteres")
    private String telefone;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public int getDificuldade() { return dificuldade; }
    public void setDificuldade(int dificuldade) { this.dificuldade = dificuldade; }

    public LocalDate getProximaRevisao() { return proximaRevisao; }
    public void setProximaRevisao(LocalDate proximaRevisao) { this.proximaRevisao = proximaRevisao; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    @Override
    public String toString() {
        return "Estudo{" +
                "id=" + id +
                ", titulo='" + titulo + '\'' +
                ", descricao='" + descricao + '\'' +
                ", dificuldade=" + dificuldade +
                ", proximaRevisao=" + proximaRevisao +
                ", email='" + email + '\'' +
                ", telefone='" + telefone + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Estudo)) return false;
        Estudo estudo = (Estudo) o;
        return Objects.equals(id, estudo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
