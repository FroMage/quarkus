package io.quarkus.coroutines.eaasync.deployment;

import java.io.ByteArrayInputStream;
import java.util.function.BiFunction;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import com.ea.async.instrumentation.Transformer;

public class EaAsyncEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new HibernateEnhancingClassVisitor(className, outputClassVisitor);
    }

    private class HibernateEnhancingClassVisitor extends ClassVisitor {

        private final String className;
        private final ClassVisitor outputClassVisitor;
        private Transformer transformer;

        public HibernateEnhancingClassVisitor(String className, ClassVisitor outputClassVisitor) {
            super(Opcodes.ASM6, new ClassWriter(0));
            this.className = className;
            this.outputClassVisitor = outputClassVisitor;
            transformer = new Transformer();
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            final ClassWriter writer = (ClassWriter) this.cv; //safe cast: cv is the the ClassWriter instance we passed to the super constructor
            //We need to convert the nice Visitor chain into a plain byte array to adapt to the Hibernate ORM
            //enhancement API:
            final byte[] inputBytes = writer.toByteArray();
            final byte[] transformedBytes = eaAsyncEnhancement(className, inputBytes);
            //            String classPath = className.replace('.', '/');
            //            new File("before/" + classPath).mkdirs();
            //            new File("after/" + classPath).mkdirs();
            //            try {
            //                Files.write(Paths.get("before/" + classPath + ".class"), inputBytes);
            //                Files.write(Paths.get("after/" + classPath + ".class"), transformedBytes);
            //            } catch (IOException e) {
            //                // TODO Auto-generated catch block
            //                e.printStackTrace();
            //            }
            //Then re-convert the transformed bytecode to not interrupt the visitor chain:
            ClassReader cr = new ClassReader(transformedBytes);
            cr.accept(outputClassVisitor, 0);
        }

        private byte[] eaAsyncEnhancement(final String className, final byte[] originalBytes) {
            byte[] enhanced;
            enhanced = transformer.instrument(Thread.currentThread().getContextClassLoader(),
                    new ByteArrayInputStream(originalBytes));
            return enhanced == null ? originalBytes : enhanced;
        }

    }

}
