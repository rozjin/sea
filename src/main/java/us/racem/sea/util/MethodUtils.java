package us.racem.sea.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MethodUtils {
    public static boolean isStatic(Method receiver) {
        return Modifier.isStatic(receiver.getModifiers());
    }

    public static MethodHandle unreflect(Method receiver, Object receiverObj) throws IllegalAccessException {
        var lookup = Modifier.isPrivate(receiver.getModifiers())
                ? MethodHandles.privateLookupIn(receiver.getDeclaringClass(), MethodHandles.lookup())
                : MethodHandles.lookup();
        var handle = lookup.unreflect(receiver);
        if (!isStatic(receiver)) handle.bindTo(receiverObj);

        return handle;
    }
}
