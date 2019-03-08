package io.quarkus.hibernate.orm.panache.deployment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.BiFunction;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import com.github.fromage.quasi.fibers.instrument.QuasiInstrumentor;

import io.quarkus.deployment.util.IoUtil;

public class QuasiEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new HibernateEnhancingClassVisitor(className, outputClassVisitor);
    }

    private class HibernateEnhancingClassVisitor extends ClassVisitor {

        private final String className;
        private final ClassVisitor outputClassVisitor;
        private QuasiInstrumentor instrumentor;

        public HibernateEnhancingClassVisitor(String className, ClassVisitor outputClassVisitor) {
            super(Opcodes.ASM6, new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS));
            this.className = className;
            this.outputClassVisitor = outputClassVisitor;
            instrumentor = new QuasiInstrumentor();
            instrumentor.setCheck(true);
            instrumentor.setDebug(true);
            instrumentor.setVerbose(true);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            final ClassWriter writer = (ClassWriter) this.cv; //safe cast: cv is the the ClassWriter instance we passed to the super constructor
            //We need to convert the nice Visitor chain into a plain byte array to adapt to the Hibernate ORM
            //enhancement API:
            final byte[] inputBytes = writer.toByteArray();
            final byte[] transformedBytes = quasiEnhancement(className, inputBytes);
            try {
                Files.write(Paths.get("before.class"), inputBytes);
                Files.write(Paths.get("after.class"), transformedBytes);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //Then re-convert the transformed bytecode to not interrupt the visitor chain:
            ClassReader cr = new ClassReader(transformedBytes);
            cr.accept(outputClassVisitor, 0);
        }

        private byte[] quasiEnhancement(final String className, final byte[] originalBytes) {
            byte[] enhanced;
            try {
                System.err.println("Instrumenting " + className);
                enhanced = instrumentor.instrumentClass(Thread.currentThread().getContextClassLoader(), className,
                        originalBytes);
                System.err.println("Instrumenting done " + className);
                return enhanced == null ? originalBytes : enhanced;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
