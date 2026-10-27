package org.example.stockhubspring.util;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** CNPJ opcional: nulo é aceito; quando informado precisa ter 14 dígitos e dígito verificador válido. */
@Documented
@Constraint(validatedBy = CnpjValido.Validador.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CnpjValido {

    String message() default "Informe um CNPJ válido (14 dígitos).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<CnpjValido, String> {
        @Override
        public boolean isValid(String valor, ConstraintValidatorContext contexto) {
            return valor == null || Documentos.cnpjValido(valor);
        }
    }
}
