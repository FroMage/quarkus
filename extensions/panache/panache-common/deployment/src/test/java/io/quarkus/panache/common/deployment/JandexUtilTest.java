package io.quarkus.panache.common.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    // Other tests
    private static final DotName SIMPLE = DotName.createSimple(Single.class.getName());
    private static final DotName MULTIPLE = DotName.createSimple(Multiple.class.getName());
    private static final DotName STRING = DotName.createSimple(String.class.getName());
    private static final DotName INTEGER = DotName.createSimple(Integer.class.getName());
    private static final DotName DOUBLE = DotName.createSimple(Double.class.getName());

    @Test
    public void testInterfaceNotInHierarchy() throws IOException {
        final Index index = index(Single.class, SingleImpl.class, Multiple.class);
        final DotName impl = DotName.createSimple(SingleImpl.class.getName());
        final List<Type> result = JandexUtil.findArgumentsToSuperType(impl, Collections.emptyList(), MULTIPLE, index);
        assertThat(result).isEmpty();
    }

    @Test
    public void testNoTypePassed() throws IOException {
        final Index index = index(Single.class, SingleImplNoType.class);
        final DotName impl = DotName.createSimple(SingleImplNoType.class.getName());
        final List<Type> result = JandexUtil.findArgumentsToSuperType(impl, Collections.emptyList(), SIMPLE, index);
        assertThat(result).isEmpty();
    }

    @Test
    public void testAbstractSingle() throws IOException {
        final Index index = index(Single.class, AbstractSingle.class);
        final DotName impl = DotName.createSimple(AbstractSingle.class.getName());
        assertThatThrownBy(() -> JandexUtil.findArgumentsToSuperType(impl, Collections.emptyList(), SIMPLE, index))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testSimplestImpl() throws IOException {
        final Index index = index(Single.class, SingleImpl.class);
        final DotName impl = DotName.createSimple(SingleImpl.class.getName());
        final List<Type> result = JandexUtil.findArgumentsToSuperType(impl, Collections.emptyList(), SIMPLE, index);
        assertThat(result).extracting("name").containsOnly(STRING);
    }

    @Test
    public void testSimplestImplWithBound() throws IOException {
        final Index index = index(SingleWithBound.class, SingleWithBoundImpl.class);
        final DotName impl = DotName.createSimple(SingleWithBoundImpl.class.getName());
        final List<Type> result = JandexUtil.findArgumentsToSuperType(impl, Collections.emptyList(),
                DotName.createSimple(SingleWithBound.class.getName()), index);
        assertThat(result).extracting("name").containsOnly(DotName.createSimple(List.class.getName()));
    }

    @Test
    public void testSimpleImplMultipleParams() throws IOException {
        final Index index = index(Multiple.class, MultipleImpl.class);
        final DotName impl = DotName.createSimple(MultipleImpl.class.getName());
        final List<Type> result = JandexUtil.findArgumentsToSuperType(impl, Collections.emptyList(), MULTIPLE, index);
        assertThat(result).extracting("name").containsExactly(INTEGER, STRING);
    }

    @Test
    public void testInverseParameterNames() throws IOException {
        final Index index = index(Multiple.class, InverseMultiple.class, InverseMultipleImpl.class);
        final DotName impl = DotName.createSimple(InverseMultipleImpl.class.getName());
        final List<Type> result = JandexUtil.findArgumentsToSuperType(impl, Collections.emptyList(), MULTIPLE, index);
        assertThat(result).extracting("name").containsExactly(DOUBLE, INTEGER);
    }

    @Test
    public void testImplExtendsSimplestImplementation() throws IOException {
        final Index index = index(Single.class, SingleImpl.class, SingleImplImpl.class);
        final DotName impl = DotName.createSimple(SingleImplImpl.class.getName());
        final List<Type> result = JandexUtil.findArgumentsToSuperType(impl, Collections.emptyList(), SIMPLE, index);
        assertThat(result).extracting("name").containsOnly(STRING);
    }

    @Test
    public void testImplementationOfInterfaceThatExtendsSimpleWithoutParam() throws IOException {
        final Index index = index(Single.class, ExtendsSimpleNoParam.class, ExtendsSimpleNoParamImpl.class);
        final DotName impl = DotName.createSimple(ExtendsSimpleNoParamImpl.class.getName());
        final List<Type> result = JandexUtil.findArgumentsToSuperType(impl, Collections.emptyList(), SIMPLE, index);
        assertThat(result).extracting("name").containsOnly(DOUBLE);
    }

    @Test
    public void testImplExtendsImplOfInterfaceThatExtendsSimpleWithoutParams() throws IOException {
        final Index index = index(Single.class, ExtendsSimpleNoParam.class, ExtendsSimpleNoParamImpl.class,
                ExtendsSimpleNoParamImplImpl.class);
        final DotName impl = DotName.createSimple(ExtendsSimpleNoParamImplImpl.class.getName());
        final List<Type> result = JandexUtil.findArgumentsToSuperType(impl, Collections.emptyList(), SIMPLE, index);
        assertThat(result).extracting("name").containsOnly(DOUBLE);
    }

    @Test
    public void testImplOfInterfaceThatExtendsSimpleWithParam() throws IOException {
        final Index index = index(Single.class, ExtendsSimpleWithParam.class, ExtendsSimpleWithParamImpl.class);
        final DotName impl = DotName.createSimple(ExtendsSimpleWithParamImpl.class.getName());
        final List<Type> result = JandexUtil.findArgumentsToSuperType(impl, Collections.emptyList(), SIMPLE, index);
        assertThat(result).extracting("name").containsOnly(INTEGER);
    }

    @Test
    public void testImplOfInterfaceThatExtendsSimpleWithParamInMultipleLevels() throws IOException {
        final Index index = index(Single.class, ExtendsSimpleWithParam.class, ExtendsExtendsSimpleWithParam.class,
                ExtendsExtendsSimpleWithParamImpl.class);
        final DotName impl = DotName.createSimple(ExtendsExtendsSimpleWithParamImpl.class.getName());
        final List<Type> result = JandexUtil.findArgumentsToSuperType(impl, Collections.emptyList(), SIMPLE, index);
        assertThat(result).extracting("name").containsOnly(DOUBLE);
    }

    @Test
    public void testImplOfInterfaceThatExtendsSimpleWithGenericParamInMultipleLevels() throws IOException {
        final Index index = index(Single.class, ExtendsSimpleWithParam.class, ExtendsExtendsSimpleWithParam.class,
                ExtendsExtendsSimpleGenericParam.class);
        final DotName impl = DotName.createSimple(ExtendsExtendsSimpleGenericParam.class.getName());
        final List<Type> result = JandexUtil.findArgumentsToSuperType(impl, Collections.emptyList(), SIMPLE, index);
        assertThat(result).extracting("name").containsOnly(DotName.createSimple(Map.class.getName()));
    }

    @Test
    public void testImplOfMultipleWithParamsInDifferentLevels() throws IOException {
        final Index index = index(Multiple.class, MultipleT1.class, ExtendsMultipleT1Impl.class);
        final DotName impl = DotName.createSimple(ExtendsMultipleT1Impl.class.getName());
        final List<Type> result = JandexUtil.findArgumentsToSuperType(impl, Collections.emptyList(), MULTIPLE, index);
        assertThat(result).extracting("name").containsOnly(INTEGER, STRING);
    }

    @Test
    public void testImplOfAbstractMultipleWithParamsInDifferentLevels() throws IOException {
        final Index index = index(Multiple.class, MultipleT1.class, AbstractMultipleT1Impl.class,
                ExtendsAbstractMultipleT1Impl.class);
        final DotName impl = DotName.createSimple(ExtendsAbstractMultipleT1Impl.class.getName());
        final List<Type> result = JandexUtil.findArgumentsToSuperType(impl, Collections.emptyList(), MULTIPLE, index);
        assertThat(result).extracting("name").containsOnly(INTEGER, STRING);
    }

    @Test
    public void testMultiplePathsToSingle() throws IOException {
        final Index index = index(Single.class, SingleImpl.class, SingleFromInterfaceAndSuperClass.class);
        final DotName impl = DotName.createSimple(SingleFromInterfaceAndSuperClass.class.getName());
        final List<Type> result = JandexUtil.findArgumentsToSuperType(impl, Collections.emptyList(), SIMPLE, index);
        assertThat(result).extracting("name").containsOnly(STRING);
    }

    @Test
    public void testExtendsAbstractClass() throws IOException {
        final DotName abstractSingle = DotName.createSimple(AbstractSingle.class.getName());
        final Index index = index(Single.class, AbstractSingle.class, AbstractSingleImpl.class,
                ExtendsAbstractSingleImpl.class);
        assertThat(JandexUtil.findArgumentsToSuperType(DotName.createSimple(AbstractSingleImpl.class.getName()), Collections.emptyList(), abstractSingle,
                index)).extracting("name").containsOnly(INTEGER);
        assertThat(JandexUtil.findArgumentsToSuperType(DotName.createSimple(ExtendsAbstractSingleImpl.class.getName()), Collections.emptyList(),
                abstractSingle, index)).extracting("name").containsOnly(INTEGER);
    }

    public interface Single<T> {
    }

    public interface SingleWithBound<T extends Collection<?>> {
    }

    public interface ExtendsSimpleNoParam extends Single<Double> {
    }

    public interface ExtendsSimpleWithParam<T> extends Single<T> {
    }

    public interface ExtendsExtendsSimpleWithParam<T> extends ExtendsSimpleWithParam<T> {
    }

    public interface Multiple<T1, T2> {
    }

    public interface InverseMultiple<T1, T2> extends Multiple<T2, T1> {
    }

    public interface MultipleT1<T> extends Multiple<Integer, T> {
    }

    public static class SingleImpl implements Single<String> {
    }

    public static class SingleImplNoType implements Single {
    }

    public static abstract class AbstractSingle<S> implements Single<S> {

    }

    public static class SingleWithBoundImpl implements SingleWithBound<List<String>> {
    }

    public static class SingleImplImpl extends SingleImpl {
    }

    public static class AbstractSingleImpl extends AbstractSingle<Integer> {
    }

    public static class ExtendsAbstractSingleImpl extends AbstractSingleImpl {
    }

    public static class MultipleImpl implements Multiple<Integer, String> {
    }

    public static class InverseMultipleImpl implements InverseMultiple<Integer, Double> {
    }

    public static class ExtendsSimpleNoParamImpl implements ExtendsSimpleNoParam {
    }

    public static class ExtendsSimpleNoParamImplImpl extends ExtendsSimpleNoParamImpl {
    }

    public static class ExtendsSimpleWithParamImpl implements ExtendsSimpleWithParam<Integer> {
    }

    public static class ExtendsExtendsSimpleWithParamImpl implements ExtendsExtendsSimpleWithParam<Double> {
    }

    public static class ExtendsExtendsSimpleGenericParam implements ExtendsExtendsSimpleWithParam<Map<String, List<String>>> {
    }

    public abstract static class AbstractMultipleT1Impl<S> implements MultipleT1<String> {
    }

    public static class ExtendsAbstractMultipleT1Impl extends AbstractMultipleT1Impl<Integer> {
    }

    public static class ExtendsMultipleT1Impl implements MultipleT1<String> {
    }

    public static class SingleFromInterfaceAndSuperClass<W> extends SingleImpl implements Single<String> {
    }

}
