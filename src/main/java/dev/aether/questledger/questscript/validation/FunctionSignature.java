package dev.aether.questledger.questscript.validation;

import dev.aether.questledger.questscript.ast.ValueType;

import java.util.List;

public record FunctionSignature(List<ValueType> argumentTypes, ValueType returnType,
                                boolean resourceIdFirstArgument) {
    public FunctionSignature {
        argumentTypes = List.copyOf(argumentTypes);
    }
}
