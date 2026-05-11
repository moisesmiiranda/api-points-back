package com.mmiranda.pointsbackapi.exception;

public class DuplicateCpfException extends RuntimeException {
    public DuplicateCpfException(String cpf) {
        super("Um cliente com o CPF " + cpf + " já está cadastrado no sistema.");
    }
}
