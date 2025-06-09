package com.example.Joinify.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidRoleValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRole {
    String message() default "Invalid role. Must be ORGANIZER or ATTENDEE";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
