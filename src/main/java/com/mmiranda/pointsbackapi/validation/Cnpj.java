package com.mmiranda.pointsbackapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A Brazilian CNPJ, with or without punctuation, with valid check digits. Null is valid. */
@Documented
@Constraint(validatedBy = CnpjValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cnpj {
    String message() default "invalid CNPJ";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
