package io.quarkus.panache.common.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.Type;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.util.IoUtil;

public class JandexUtilTest {
    
    interface Repo<T> {}
    
    class DirectRepo implements Repo<Integer>{}
    class IndirectRepo extends DirectRepo{}
    
    class GenericRepo<X> implements Repo<X>{}
    class IndirectGenericRepo extends GenericRepo<Integer>{}

    class GenericArrayRepo<X> implements Repo<X[]>{}
    class ArrayRepo extends GenericArrayRepo<Integer>{}

    class GenericCompositeRepo<X> implements Repo<Repo<X>>{}
    class CompositeRepo extends GenericCompositeRepo<Integer>{}

    class BoundedRepo<X extends A> implements Repo<X>{}
    class ErasedRepo1 extends BoundedRepo{}

    interface A{}
    interface B{}
    
    class MultiBoundedRepo<X extends A & B> implements Repo<X>{}
    class ErasedRepo2 extends MultiBoundedRepo{}

    @Test
    public void test() throws IOException {
        Index index = index(DirectRepo.class, IndirectRepo.class, GenericRepo.class, IndirectGenericRepo.class, Repo.class,
                ArrayRepo.class, GenericArrayRepo.class, GenericCompositeRepo.class, CompositeRepo.class,
                BoundedRepo.class, ErasedRepo1.class, MultiBoundedRepo.class, ErasedRepo2.class);
        
        checkRepoArg(index, DirectRepo.class, Repo.class, Integer.class);
        checkRepoArg(index, IndirectRepo.class, Repo.class, Integer.class);

        checkRepoArg(index, IndirectGenericRepo.class, Repo.class, Integer.class);

        checkRepoArg(index, ArrayRepo.class, Repo.class, Integer[].class);

        checkRepoArg(index, CompositeRepo.class, Repo.class, Repo.class.getName()+"<java.lang.Integer>");

        checkRepoArg(index, ErasedRepo1.class, Repo.class, A.class);
        checkRepoArg(index, ErasedRepo2.class, Repo.class, A.class);
    }

    private void checkRepoArg(Index index, Class<?> baseClass, Class<?> soughtClass, Class<?> expectedArg) {
        // stef
        List<Type> args = JandexUtil.findArgumentsToSuperType(name(baseClass), Collections.emptyList(), name(soughtClass), index);
        Assert.assertNotNull(args);
        Assert.assertEquals(1, args.size());
        Assert.assertEquals(name(expectedArg), args.get(0).name());
        
        // georgios
        args = io.quarkus.deployment.util.JandexUtil.resolveTypeParameters(name(baseClass), name(soughtClass), index);
        Assert.assertNotNull(args);
        Assert.assertEquals(1, args.size());
        Assert.assertEquals(name(expectedArg), args.get(0).name());
    }

    private void checkRepoArg(Index index, Class<?> baseClass, Class<?> soughtClass, String expectedArg) {
        // stef
        List<Type> args = JandexUtil.findArgumentsToSuperType(name(baseClass), Collections.emptyList(), name(soughtClass), index);
        Assert.assertNotNull(args);
        Assert.assertEquals(1, args.size());
        Assert.assertEquals(expectedArg, args.get(0).toString());

        // georgios
        args = io.quarkus.deployment.util.JandexUtil.resolveTypeParameters(name(baseClass), name(soughtClass), index);
        Assert.assertNotNull(args);
        Assert.assertEquals(1, args.size());
        Assert.assertEquals(expectedArg, args.get(0).toString());
    }

    private static DotName name(Class<?> klass) {
        return DotName.createSimple(klass.getName());
    }

    private Index index(Class<?>... classes) throws IOException {
        Indexer idx = new Indexer();
        for (Class<?> klass : classes) {
            try(InputStream c = IoUtil.readClass(JandexUtilTest.class.getClassLoader(), klass.getName())){
                idx.index(c);
            }
        }
        return idx.complete();
    }
}
