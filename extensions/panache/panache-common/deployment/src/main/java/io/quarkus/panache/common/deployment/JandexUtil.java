package io.quarkus.panache.common.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType.Primitive;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.jboss.jandex.WildcardType;
import org.objectweb.asm.Opcodes;

import io.quarkus.panache.common.impl.GenerateBridge;

public class JandexUtil {
    public static final DotName DOTNAME_GENERATE_BRIDGE = DotName.createSimple(GenerateBridge.class.getName());
    private static final DotName DOTNAME_OBJECT = DotName.createSimple(Object.class.getName());

    public static String getSignature(MethodInfo method, Function<String, String> typeArgMapper) {
        List<Type> parameters = method.parameters();

        StringBuilder signature = new StringBuilder("");
        for (TypeVariable typeVariable : method.typeParameters()) {
            if (signature.length() == 0)
                signature.append("<");
            else
                signature.append(",");
            signature.append(typeVariable.identifier()).append(":");
            // FIXME: only use the first bound
            toSignature(signature, typeVariable.bounds().get(0), typeArgMapper, false);
        }
        if (signature.length() > 0)
            signature.append(">");
        signature.append("(");
        for (Type type : parameters) {
            toSignature(signature, type, typeArgMapper, false);
        }
        signature.append(")");
        toSignature(signature, method.returnType(), typeArgMapper, false);
        return signature.toString();
    }

    public static String getDescriptor(MethodInfo method, Function<String, String> typeArgMapper) {
        List<Type> parameters = method.parameters();

        StringBuilder descriptor = new StringBuilder("(");
        for (Type type : parameters) {
            toSignature(descriptor, type, typeArgMapper, true);
        }
        descriptor.append(")");
        toSignature(descriptor, method.returnType(), typeArgMapper, true);
        return descriptor.toString();
    }

    static void toSignature(StringBuilder sb, Type type, Function<String, String> typeArgMapper, boolean erased) {
        switch (type.kind()) {
            case ARRAY:
                ArrayType arrayType = type.asArrayType();
                for (int i = 0; i < arrayType.dimensions(); i++)
                    sb.append("[");
                toSignature(sb, arrayType.component(), typeArgMapper, erased);
                break;
            case CLASS:
                sb.append("L");
                sb.append(type.asClassType().name().toString().replace('.', '/'));
                sb.append(";");
                break;
            case PARAMETERIZED_TYPE:
                ParameterizedType parameterizedType = type.asParameterizedType();
                sb.append("L");
                // FIXME: support owner type
                sb.append(parameterizedType.name().toString().replace('.', '/'));
                if (!erased && !parameterizedType.arguments().isEmpty()) {
                    sb.append("<");
                    List<Type> arguments = parameterizedType.arguments();
                    for (int i = 0; i < arguments.size(); i++) {
                        Type argType = arguments.get(i);
                        toSignature(sb, argType, typeArgMapper, erased);
                    }
                    sb.append(">");
                }
                sb.append(";");
                break;
            case PRIMITIVE:
                Primitive primitive = type.asPrimitiveType().primitive();
                switch (primitive) {
                    case BOOLEAN:
                        sb.append('Z');
                        break;
                    case BYTE:
                        sb.append('B');
                        break;
                    case CHAR:
                        sb.append('C');
                        break;
                    case DOUBLE:
                        sb.append('D');
                        break;
                    case FLOAT:
                        sb.append('F');
                        break;
                    case INT:
                        sb.append('I');
                        break;
                    case LONG:
                        sb.append('J');
                        break;
                    case SHORT:
                        sb.append('S');
                        break;
                }
                break;
            case TYPE_VARIABLE:
                TypeVariable typeVariable = type.asTypeVariable();
                String mappedSignature = typeArgMapper.apply(typeVariable.identifier());
                if (mappedSignature != null)
                    sb.append(mappedSignature);
                else if (erased)
                    toSignature(sb, typeVariable.bounds().get(0), typeArgMapper, erased);
                else
                    sb.append("T").append(typeVariable.identifier()).append(";");
                break;
            case UNRESOLVED_TYPE_VARIABLE:
                // FIXME: ??
                break;
            case VOID:
                sb.append("V");
                break;
            case WILDCARD_TYPE:
                if (!erased) {
                    sb.append("*");
                }
                break;
            default:
                break;

        }
    }

    public static int getReturnInstruction(String typeDescriptor) {
        switch (typeDescriptor) {
            case "Z":
            case "B":
            case "C":
            case "S":
            case "I":
                return Opcodes.IRETURN;
            case "J":
                return Opcodes.LRETURN;
            case "F":
                return Opcodes.FRETURN;
            case "D":
                return Opcodes.DRETURN;
            case "V":
                return Opcodes.RETURN;
            default:
                return Opcodes.ARETURN;
        }
    }

    public static List<org.jboss.jandex.Type> findArgumentsToSuperType(DotName currentClass, List<org.jboss.jandex.Type> args,
            DotName soughtSuperType, IndexView index) {
        // stop if we didn't find it
        if (currentClass.equals(DOTNAME_OBJECT)) {
            return null;
        }

        final ClassInfo classByName = index.getClassByName(currentClass);
        // class not indexed
        if(classByName == null)
            return null;

        // build a map of this classe's type params to their arguments
        Map<TypeVariable, org.jboss.jandex.Type> typeArgs = new HashMap<>();
        List<TypeVariable> typeParameters = classByName.typeParameters();
        for (int i = 0; i < typeParameters.size(); i++) {
            TypeVariable typeVariable = typeParameters.get(i);
            // default to first bound instead?
            org.jboss.jandex.Type arg;
            if (i < args.size())
                arg = args.get(i);
            else
                arg = typeVariable;
            typeArgs.put(typeVariable, arg);
        }

        // look at the interfaces for a direct implementation
        for (org.jboss.jandex.Type type : classByName.interfaceTypes()) {
            if (type.name().equals(soughtSuperType)) {
                // found the type args
                return substitute(type.asParameterizedType().arguments(), typeArgs);
            }
            // look at super-interfaces
            // FIXME: add cache to avoid visiting the same interface more than once
            List<org.jboss.jandex.Type> superArgs = findSuperArgs(type, typeArgs);
            List<org.jboss.jandex.Type> ret = findArgumentsToSuperType(type.name(), superArgs, soughtSuperType, index);
            if(ret != null)
                return ret;
        }
        // look at super-type
        List<org.jboss.jandex.Type> superArgs = findSuperArgs(classByName.superClassType(), typeArgs);
        if(classByName.superName().equals(soughtSuperType))
            return superArgs;
        return findArgumentsToSuperType(classByName.superName(), superArgs, soughtSuperType, index);
    }

    private static List<org.jboss.jandex.Type> findSuperArgs(org.jboss.jandex.Type superClassType,
            Map<TypeVariable, org.jboss.jandex.Type> typeArgs) {
        switch (superClassType.kind()) {
            case CLASS:
                return Collections.emptyList();
            case PARAMETERIZED_TYPE:
                return substitute(superClassType.asParameterizedType().arguments(), typeArgs);
            default:
                throw new RuntimeException("Invalid supertype kind: " + superClassType);
        }
    }

    private static List<org.jboss.jandex.Type> substitute(List<org.jboss.jandex.Type> types,
            Map<TypeVariable, org.jboss.jandex.Type> typeArgs){
        List<org.jboss.jandex.Type> substitutedArgs = new ArrayList<>(types);
        for (int i = 0; i < substitutedArgs.size(); i++) {
            substitutedArgs.set(i, substitute(substitutedArgs.get(i), typeArgs));
        }
        return substitutedArgs;
    }
    
    private static org.jboss.jandex.Type substitute(org.jboss.jandex.Type type,
            Map<TypeVariable, org.jboss.jandex.Type> typeArgs) {
        switch (type.kind()) {
            case ARRAY:
                ArrayType arrayType = type.asArrayType();
                org.jboss.jandex.Type substitutedComponent = substitute(arrayType.component(), typeArgs);
                return ArrayType.create(substitutedComponent, arrayType.dimensions());
            case TYPE_VARIABLE:
                org.jboss.jandex.Type substitutedArgument = typeArgs.get(type.asTypeVariable());
                return substitutedArgument != null ? substitutedArgument : type;
            case VOID:
            case PRIMITIVE:
            case CLASS:
                return type;
            case PARAMETERIZED_TYPE:
                ParameterizedType parameterizedType = type.asParameterizedType();
                org.jboss.jandex.Type substitutedOwner = parameterizedType.owner() != null
                        ? substitute(parameterizedType.owner(), typeArgs)
                        : null;
                List<org.jboss.jandex.Type> substitutedArguments = substitute(parameterizedType.arguments(), typeArgs);
                return ParameterizedType.create(parameterizedType.name(), substitutedArguments.toArray(new org.jboss.jandex.Type[0]), substitutedOwner);
            case UNRESOLVED_TYPE_VARIABLE:
                return type;
            case WILDCARD_TYPE:
                WildcardType wildcardType = type.asWildcardType();
                org.jboss.jandex.Type superBound = wildcardType.superBound();
                // do erasure to bounds
                if (superBound != null)
                    return substitute(superBound, typeArgs);
                return substitute(wildcardType.extendsBound(), typeArgs);
            default:
                throw new RuntimeException("Invalid supertype kind: " + type);
        }
    }
}
