package me.mdbell.awtea.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ArrayUtils {

    public <T> boolean contains(T[] array, T value) {
        for (T element : array) {
            if (element.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
