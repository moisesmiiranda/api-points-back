package com.mmiranda.pointsbackapi.validation;

import jakarta.validation.groups.Default;

/**
 * Validation group for rules that only apply when a resource is created (required fields).
 * Extends {@link Default} so create requests also run the field-format rules, while partial
 * updates (PUT with only some fields) validate formats only.
 */
public interface OnCreate extends Default {
}
