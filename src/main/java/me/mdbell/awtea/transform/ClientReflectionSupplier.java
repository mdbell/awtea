package me.mdbell.awtea.transform;


import org.teavm.classlib.ReflectionContext;
import org.teavm.classlib.ReflectionSupplier;
import org.teavm.model.ClassReader;
import org.teavm.model.MethodDescriptor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ClientReflectionSupplier implements ReflectionSupplier {

    private static final String[] ALLOWED_CLASSES = new String[]{
            // Audio Line
//            MidiPlayer.class.getName(),
    };

    @Override
    public Collection<String> getAccessibleFields(ReflectionContext context, String className) {
        return List.of();
    }

    @Override
    public Collection<MethodDescriptor> getAccessibleMethods(ReflectionContext context, String className) {
        List<MethodDescriptor> constructors = new LinkedList<>();

        boolean allowed = false;
        for (String allowedClass : ALLOWED_CLASSES) {
            if (allowedClass.equals(className)) {
                allowed = true;
                break;
            }
        }

        if (!allowed) {
            return constructors;
        }

        // only add no-arg constructors
        ClassReader cls = context.getClassSource().get(className);

        cls.getMethods().stream().filter(methodReader -> methodReader.getName().equals("<init>") && methodReader.parameterCount() == 0)
                .forEach(methodReader -> constructors.add(methodReader.getDescriptor()));

        return constructors;
    }
}
