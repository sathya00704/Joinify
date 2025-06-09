package com.example.Joinify.validation;

import com.example.Joinify.entity.UserRole;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidRoleValidator implements ConstraintValidator<ValidRole, UserRole> {

    @Override
    public void initialize(ValidRole constraintAnnotation) {
        // Initialization if needed
    }

    @Override
    public boolean isValid(UserRole role, ConstraintValidatorContext context) {
        if (role == null) {
            return false;
        }
        return role == UserRole.ORGANIZER || role == UserRole.ATTENDEE;
    }
}
