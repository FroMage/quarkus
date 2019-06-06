package io.quarkus.panache.rx.deployment;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;


public class MultiplexingAnnotationVisitor extends AnnotationVisitor {

    private AnnotationVisitor fakeAnnotationVisitor;

    public MultiplexingAnnotationVisitor(AnnotationVisitor fakeAnnotationVisitor, AnnotationVisitor secondVisitor) {
        super(Opcodes.ASM6, secondVisitor);
        this.fakeAnnotationVisitor = fakeAnnotationVisitor;
    }

    @Override
    public void visit(String name, Object value) {
        fakeAnnotationVisitor.visit(name, value);
        super.visit(name, value);
    }
    
    @Override
    public AnnotationVisitor visitAnnotation(String name, String descriptor) {
        return new MultiplexingAnnotationVisitor(fakeAnnotationVisitor.visitAnnotation(name, descriptor), super.visitAnnotation(name, descriptor));
    }
    
    @Override
    public AnnotationVisitor visitArray(String name) {
        return new MultiplexingAnnotationVisitor(fakeAnnotationVisitor.visitArray(name), super.visitArray(name));
    }
    
    @Override
    public void visitEnd() {
        fakeAnnotationVisitor.visitEnd();
        super.visitEnd();
    }
    
    @Override
    public void visitEnum(String name, String descriptor, String value) {
        fakeAnnotationVisitor.visitEnum(name, descriptor, value);
        super.visitEnum(name, descriptor, value);
    }
}
