package com.outbrain.ob1k.server;


import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by aronen on 2/12/14.
 * extract an ordered list of param names from all given methods
 * assumes that each method is non-static and is not a constructor
 */
public class MethodParamNamesExtractor {

  public static Map<Method, List<String>> extract(final Class<?> type, final Collection<Method> methods) throws IOException {

    final InputStream classBytes = MethodParamNamesExtractor.class.getClassLoader().getResourceAsStream(toResourceName(type));
    final ClassReader cr = new ClassReader(classBytes);

    final Map<String, Method> methodsSignature = new HashMap<>();
    for (final Method method: methods) {
      methodsSignature.put(method.getName(), method);
    }

    if (methodsSignature.size() != new HashSet<>(methods).size()) {
      throw new IllegalArgumentException("method names must be unique in service: " + type.getName());
    }

    final Map<Method, List<String>> result = new HashMap<>();
    cr.accept(new ClassVisitor(Opcodes.ASM4) {
      public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        final Method method = methodsSignature.get(name);
        if(method != null) {
          final Type[] types = Type.getArgumentTypes(desc);
          final List<String> names = new ArrayList<>(types.length);
          result.put(method, names);

          return new MethodVisitor(Opcodes.ASM4) {
            @Override
            public void visitLocalVariable(final String name, final String description, final String signature, final Label start, final Label end, final int index) {
              // zero index is always this in non-static, non constructor methods.
              // Notes: don't ask me why, but ASM doesn't always pass in indexes from 0 to num args + 1, and sometimes weird 'input' name is passed in at the end. Meh.
              if (index > 0 && names.size() < types.length) {
                names.add(name);
              }
            }
          };
        } else {
          return super.visitMethod(access, name, desc, signature, exceptions);
        }
      }
    }, 0);

    return result;
  }

  private static String toResourceName(final Class<?> type) {
    return type.getName().replace('.', '/') + ".class";
  }
}
