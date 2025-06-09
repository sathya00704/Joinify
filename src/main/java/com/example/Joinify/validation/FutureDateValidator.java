package com.example.Joinify.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;

public class FutureDateValidator implements ConstraintValidator<FutureDate, LocalDateTime> {

    @Override
    public void initialize(FutureDate constraintAnnotation) {
        // Initialization if needed
    }

    @Override
    public boolean isValid(LocalDateTime dateTime, ConstraintValidatorContext context) {
        if (dateTime == null) {
            return true; // Let @NotNull handle null validation
        }
        return dateTime.isAfter(LocalDateTime.now());
    }
}
