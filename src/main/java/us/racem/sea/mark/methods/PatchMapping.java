package us.racem.sea.mark.methods;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PatchMapping {
    String path();
    RequestMethod[] methods = new RequestMethod[] {RequestMethod.PATCH};
}
