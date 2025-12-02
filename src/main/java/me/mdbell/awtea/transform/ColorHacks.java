package me.mdbell.awtea.transform;

import org.teavm.model.*;
import org.teavm.model.instructions.*;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class ColorHacks implements ClassHolderTransformer {

    private static final String COLOR_CLASS_NAME = Color.class.getName();

    @Override
    public void transformClass(ClassHolder classHolder, ClassHolderTransformerContext classHolderTransformerContext) {
        // by the time this run teavm has already replaced the system `Color` class
        // with their own, but theirs only has BLACK and WHITE, none of the other colors vars.
        if (classHolder.getName().equals(COLOR_CLASS_NAME)) {
            try {
                installFields(classHolder, classHolderTransformerContext);
            } catch (IllegalAccessException e) {
				System.out.println("Failed to install Color fields:");
                throw new RuntimeException(e);
            }
        }
    }

    private void installFields(ClassHolder clazz, ClassHolderTransformerContext context) throws IllegalAccessException {
        Class<?> colorClass = Color.class;

        for (Field f : colorClass.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || f.getType() != colorClass) {
                continue; // we only want to get fields that are static Colors, we don't care about the rest.
            }
            FieldHolder holder = clazz.getField(f.getName());
            if (holder != null) {
                continue; // they already define some in the base class, so we skip it
            }
            Color value = (Color) f.get(null);
            holder = new FieldHolder(f.getName());
            EnumSet<ElementModifier> modifiers = holder.getModifiers();
            modifiers.add(ElementModifier.FINAL);
            modifiers.add(ElementModifier.STATIC);
            holder.setType(ValueType.object(COLOR_CLASS_NAME));
            holder.setLevel(AccessLevel.PUBLIC);
            clazz.addField(holder);
            installColor(clazz, holder, value.getRed(), value.getGreen(), value.getBlue());
        }
    }

    private void installColor(ClassHolder clazz, FieldHolder field, int r, int g, int b) {
        MethodHolder constructor = clazz.getMethod(MethodDescriptor.parse("<init>(III)V"));
        MethodHolder staticConstructor = clazz.getMethod(MethodDescriptor.parse("<clinit>()V"));
        Program program = staticConstructor.getProgram();

        // index 0 is generally reserved for this, so we don't wanna use it
        Variable colorVar = program.variableAt(1);
        Variable redVar = program.variableAt(2);
        Variable greenVar = program.variableAt(3);
        Variable blueVar = program.variableAt(4);

        // we skip the first basic block as it's the code that calls the super constructor
        BasicBlock block = program.basicBlockAt(1);

        List<Instruction> instructionsToAdd = new ArrayList<>();

        // create the color instance
        ConstructInstruction constructColor = new ConstructInstruction();
        constructColor.setType(COLOR_CLASS_NAME);
        constructColor.setReceiver(colorVar);
        instructionsToAdd.add(constructColor);

        // setup the vars on the stack
        instructionsToAdd.add(createConstant(redVar, r));
        instructionsToAdd.add(createConstant(greenVar, g));
        instructionsToAdd.add(createConstant(blueVar, b));

        // create the object
        InvokeInstruction invoke = new InvokeInstruction();
        invoke.setType(InvocationType.SPECIAL);
        invoke.setMethod(constructor.getReference());
        invoke.setInstance(colorVar);
        invoke.setArguments(redVar, greenVar, blueVar);
        instructionsToAdd.add(invoke);

        // store the object to the field
        PutFieldInstruction put = new PutFieldInstruction();
        put.setField(field.getReference());
        put.setValue(colorVar);
        instructionsToAdd.add(put);

        block.addFirstAll(instructionsToAdd);
    }

    private IntegerConstantInstruction createConstant(Variable var, int constant) {
        IntegerConstantInstruction res = new IntegerConstantInstruction();
        res.setReceiver(var);
        res.setConstant(constant);
        return res;
    }
}
