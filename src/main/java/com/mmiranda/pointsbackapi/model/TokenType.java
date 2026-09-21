package com.mmiranda.pointsbackapi.model;

/** Why a set-password link was issued: the user asked to recover it, or a manager invited the user. */
public enum TokenType {
    RESET, INVITE
}
