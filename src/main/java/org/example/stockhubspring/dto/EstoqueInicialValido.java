package org.example.stockhubspring.dto;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Limite do estoque inicial (máximo 1.000.000), valendo só no CADASTRO. Na edição a quantidade do
 * formulário é ignorada (RN-03) e traz o saldo atual, que pode passar do limite; por isso a regra
 * é da classe (depende de {@code id}) e o erro é publicado no campo {@code quantidade}.
 */
@Documented
@Constraint(validatedBy = EstoqueInicialValido.Validador.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EstoqueInicialValido {

    String message() default "O estoque inicial deve ser no máximo 1.000.000.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<EstoqueInicialValido, ProdutoForm> {

        @Override
        public boolean isValid(ProdutoForm form, ConstraintValidatorContext contexto) {
            if (form == null || form.getId() != null || form.getQuantidade() == null
                    || form.getQuantidade() <= ProdutoForm.ESTOQUE_INICIAL_MAXIMO) {
                return true;
            }
            contexto.disableDefaultConstraintViolation();
            contexto.buildConstraintViolationWithTemplate(contexto.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("quantidade")
                    .addConstraintViolation();
            return false;
        }
    }
}
