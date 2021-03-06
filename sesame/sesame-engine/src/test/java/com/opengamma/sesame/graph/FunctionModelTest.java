/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Provider;

import org.testng.annotations.Test;
import org.threeten.bp.LocalDate;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.opengamma.core.config.Config;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.function.scenarios.ScenarioArgument;
import com.opengamma.sesame.function.scenarios.ScenarioFunction;
import com.opengamma.sesame.graph.convert.DefaultArgumentConverter;
import com.opengamma.util.test.TestGroup;

/**
 * Test.
 */
@Test(groups = TestGroup.UNIT)
public class FunctionModelTest {

  private static final String INFRASTRUCTURE_COMPONENT = "some pretend infrastructure";
  private static final FunctionMetadata METADATA = EngineUtils.createMetadata(TestFn.class, "foo");

  @Test
  public void basicImpl() {
    FunctionModelConfig config = config(implementations(TestFn.class, BasicImpl.class));
    FunctionModel functionModel = FunctionModel.forFunction(METADATA, config);
    TestFn fn = (TestFn) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    assertTrue(fn instanceof BasicImpl);
  }

  @Test
  public void infrastructure() {
    ComponentMap infrastructure = ComponentMap.of(ImmutableMap.<Class<?>, Object>of(String.class,
                                                                                    INFRASTRUCTURE_COMPONENT));
    FunctionModelConfig config = config(implementations(TestFn.class, InfrastructureImpl.class));
    FunctionModel functionModel = FunctionModel.forFunction(METADATA, config, infrastructure.getComponentTypes());
    TestFn fn = (TestFn) functionModel.build(new FunctionBuilder(), infrastructure).getReceiver();
    assertTrue(fn instanceof InfrastructureImpl);
    //noinspection ConstantConditions
    assertEquals(INFRASTRUCTURE_COMPONENT, ((InfrastructureImpl) fn)._infrastructureComponent);
  }

  @Test
  public void functionCallingOtherFunction() {
    FunctionModelConfig config = config(implementations(TestFn.class, CallsOtherFn.class,
                                                        CollaboratorFn.class, Collaborator.class));
    FunctionModel functionModel = FunctionModel.forFunction(METADATA, config);
    TestFn fn = (TestFn) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    assertTrue(fn instanceof CallsOtherFn);
    //noinspection ConstantConditions
    assertTrue(((CallsOtherFn) fn)._collaborator instanceof Collaborator);
  }

  @Test
  public void concreteTypes() {
    FunctionMetadata metadata = EngineUtils.createMetadata(Concrete1.class, "foo");
    FunctionModel functionModel = FunctionModel.forFunction(metadata);
    Concrete1 fn = (Concrete1) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    assertNotNull(fn._concrete);
  }

  public void provider() {
    FunctionMetadata metadata = EngineUtils.createMetadata(PrivateConstructor.class, "getName");
    String providerName = "the provider name";
    FunctionModelConfig config = config(implementations(PrivateConstructor.class, PrivateConstructorProvider.class),
                                        arguments(
                                            function(PrivateConstructorProvider.class,
                                                     argument("providerName", providerName))));
    FunctionModel functionModel = FunctionModel.forFunction(metadata, config);
    PrivateConstructor fn = (PrivateConstructor) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    assertEquals(providerName, fn.getName());
  }

  @Test
  public void decorators() {
    NodeDecorator decorator = new NodeDecorator() {

      @Override
      public FunctionModelNode decorateNode(final FunctionModelNode node) {
        return new DependentNode(Object.class, null, node) {
          @Override
          protected Object doCreate(ComponentMap componentMap, List<Object> dependencies, FunctionIdProvider idProvider) {
            final TestFn fn = (TestFn) dependencies.get(0);
            return new TestFn() {
              @Override
              public Object foo() {
                return Lists.newArrayList("decorated", fn.foo());
              }
            };
          }
          @Override
          protected String prettyPrintLine() {
            return "";
          }
        };
      }
    };
    FunctionModelConfig config = config(implementations(TestFn.class, BasicImpl.class));
    FunctionModel functionModel = FunctionModel.forFunction(METADATA,
                                                            config,
                                                            ComponentMap.EMPTY.getComponentTypes(),
                                                            decorator);
    TestFn fn = (TestFn) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    // the basic method just returns "foo"
    assertEquals(Lists.newArrayList("decorated", "foo"), fn.foo());
  }

  @Test
  public void buildDirectly1() {
    Concrete1 fn = FunctionModel.build(Concrete1.class);
    assertNotNull(fn);
  }

  @Test
  public void buildDirectly2() {
    FunctionModelConfig config = config(implementations(TestFn.class, BasicImpl.class));
    TestFn fn = FunctionModel.build(TestFn.class, config);
    assertTrue(fn instanceof BasicImpl);
  }

  @Test
  public void buildDirectly3() {
    ComponentMap infrastructure = ComponentMap.of(ImmutableMap.<Class<?>, Object>of(String.class,
                                                                                    INFRASTRUCTURE_COMPONENT));
    FunctionModelConfig config = config(implementations(TestFn.class, InfrastructureImpl.class));
    TestFn fn = FunctionModel.build(TestFn.class, config, infrastructure);
    assertTrue(fn instanceof InfrastructureImpl);
    //noinspection ConstantConditions
    assertEquals(INFRASTRUCTURE_COMPONENT, ((InfrastructureImpl) fn)._infrastructureComponent);
  }

  @Test
  public void noVisibleConstructors() {
    FunctionMetadata metadata = EngineUtils.createMetadata(PrivateConstructor.class, "getName");
    FunctionModelConfig config = config(arguments(function(PrivateConstructor.class, argument("name", "the name"))));
    FunctionModel functionModel = FunctionModel.forFunction(metadata, config);
    assertFalse(functionModel.isValid());
  }

  @Test
  public void infrastructureNotFound() {
    FunctionModelConfig config = config(implementations(TestFn.class, InfrastructureImpl.class));
    FunctionModel functionModel = FunctionModel.forFunction(METADATA, config, ComponentMap.EMPTY.getComponentTypes());
    assertFalse(functionModel.isValid());
  }

  @Test
  public void multipleInjectableConstructors() {
    FunctionMetadata metadata = EngineUtils.createMetadata(NoSuitableConstructor.class, "foo");
    FunctionModel functionModel = FunctionModel.forFunction(metadata);
    assertFalse(functionModel.isValid());
  }

  /** test that error nodes are marked in the pretty printed output */
  @Test
  public void prettyPrintErrors() {
    FunctionModelConfig config = config(implementations(TestFn.class, CallsOtherFn.class,
                                                        CollaboratorFn.class, BrokenCollaborator.class));
    FunctionModel functionModel = FunctionModel.forFunction(METADATA, config);
    String tree = functionModel.prettyPrint();
    String[] lines = tree.split("\n");
    assertEquals(4, lines.length);
    System.out.println(tree);
    assertTrue(lines[3].startsWith("->"));
  }

  /** test that non-error nodes aren't marked in the pretty printed output */
  @Test
  public void prettyPrintNoErrors() {
    FunctionModelConfig config = config(implementations(TestFn.class, CallsOtherFn.class,
                                                        CollaboratorFn.class, Collaborator.class));
    FunctionModel functionModel = FunctionModel.forFunction(METADATA, config);
    String tree = functionModel.prettyPrint();
    String[] lines = tree.split("\n");
    assertEquals(3, lines.length);
    assertFalse(lines[2].startsWith("->"));
    System.out.println(tree);
  }

  @Test
  public void decoratorFunction() {
    FunctionModelConfig config = config(implementations(Fn.class, Impl.class));
    FunctionModelConfig decoratedConfig = config.decoratedWith(Decorator1.class);
    Fn fn = FunctionModel.build(Fn.class, decoratedConfig);
    assertEquals("2", fn.foo(1));
  }

  @Test
  public void decoratorFunctions() {
    FunctionModelConfig config = config(implementations(Fn.class, Impl.class));
    FunctionModelConfig decoratedConfig = config.decoratedWith(Decorator2.class).decoratedWith(Decorator1.class);
    Fn fn = FunctionModel.build(Fn.class, decoratedConfig);
    assertEquals("5", fn.foo(2));
  }

  @Test
  public void decoratorFunctionsReversed() {
    FunctionModelConfig config = config(implementations(Fn.class, Impl.class));
    FunctionModelConfig decoratedConfig = config.decoratedWith(Decorator1.class).decoratedWith(Decorator2.class);
    Fn fn = FunctionModel.build(Fn.class, decoratedConfig);
    assertEquals("6", fn.foo(2));
  }

  @Test
  public void linkWithWrongType() {
    FunctionModelConfig config = config(arguments(function(WithLink.class, argument("arg", ConfigLink.resolved(123)))));
    FunctionMetadata metadata = EngineUtils.createMetadata(WithLink.class, "foo");
    FunctionModel model = FunctionModel.forFunction(metadata, config);
    assertFalse(model.isValid());
    FunctionModelNode node = model.getRoot().getDependencies().get(0);
    assertTrue(node instanceof IncompatibleArgumentTypeNode);
  }

  @Test
  public void argumentWithWrongType() {
    FunctionModelConfig config = config(arguments(function(WithLink.class, argument("arg", 123))));
    FunctionMetadata metadata = EngineUtils.createMetadata(WithLink.class, "foo");
    FunctionModel model = FunctionModel.forFunction(metadata, config);
    assertFalse(model.isValid());
    FunctionModelNode node = model.getRoot().getDependencies().get(0);
    assertTrue(node instanceof IncompatibleArgumentTypeNode);
  }

  @Config
  public static class WithLink {

    public WithLink(String arg) {
    }

    @Output("Foo")
    public Object foo() {
      return null;
    }
  }

  @Test
  public void missingArgument() {
    FunctionMetadata metadata = EngineUtils.createMetadata(WithLink.class, "foo");
    FunctionModel model = FunctionModel.forFunction(metadata, FunctionModelConfig.EMPTY);
    assertFalse(model.isValid());
    FunctionModelNode node = model.getRoot().getDependencies().get(0);
    assertTrue(node instanceof MissingArgumentNode);
  }

  @Test
  public void missingConfigArgument() {
    FunctionMetadata metadata = EngineUtils.createMetadata(UsesConfigArg.class, "foo");
    FunctionModel model = FunctionModel.forFunction(metadata, FunctionModelConfig.EMPTY);
    assertFalse(model.isValid());
    FunctionModelNode node = model.getRoot().getDependencies().get(0);
    assertTrue(node instanceof MissingConfigNode);
  }

  public static class UsesConfigArg {

    public UsesConfigArg(ConfigArg configArg) {
    }

    @Output("Foo")
    public Object foo() {
      return null;
    }
  }

  @Config
  public static class ConfigArg {

  }

  @Test
  public void convertArgumentsFromStrings() {
    FunctionMetadata metadata = EngineUtils.createMetadata(ConvertArgsFromStrings.class, "foo");
    FunctionModelConfig config =
        config(
            arguments(
                function(
                    ConvertArgsFromStrings.class,
                    argument("integers", "1, 2, 3"),
                    argument("date", "2011-03-08"))));
    FunctionModel model = FunctionModel.forFunction(metadata, config, Collections.<Class<?>>emptySet(),
                                                    NodeDecorator.IDENTITY, new DefaultArgumentConverter());
    assertTrue(model.isValid());
    ConvertArgsFromStrings fn = (ConvertArgsFromStrings) model.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    assertEquals(Lists.newArrayList(1, 2, 3), fn._integers);
    assertEquals(LocalDate.of(2011, 3, 8), fn._date);
  }

  @Test
  public void conversionErrors() {
    FunctionMetadata metadata = EngineUtils.createMetadata(ConvertArgsFromStrings.class, "foo");
    FunctionModelConfig config =
        config(
            arguments(
                function(
                    ConvertArgsFromStrings.class,
                    argument("integers", "1, 2, A"),
                    argument("date", "2011-03-08-foo"))));
    FunctionModel model = FunctionModel.forFunction(metadata, config, Collections.<Class<?>>emptySet(),
                                                    NodeDecorator.IDENTITY, new DefaultArgumentConverter());
    assertFalse(model.isValid());
    List<FunctionModelNode> dependencies = model.getRoot().getDependencies();
    assertEquals(2, dependencies.size());
    assertTrue(dependencies.get(0) instanceof ArgumentConversionErrorNode);
    assertTrue(dependencies.get(1) instanceof ArgumentConversionErrorNode);
    assertEquals("1, 2, A", ((ArgumentConversionErrorNode) dependencies.get(0)).getValue());
    assertEquals("2011-03-08-foo", ((ArgumentConversionErrorNode) dependencies.get(1)).getValue());
  }

  public static class ConvertArgsFromStrings {

    private final List<Integer> _integers;
    private final LocalDate _date;

    public ConvertArgsFromStrings(List<Integer> integers, LocalDate date) {
      _integers = integers;
      _date = date;
    }

    @Output("Foo")
    public Object foo() {
      return null;
    }
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nodeAndMetadataMismatch() {
    FunctionMetadata metadata = EngineUtils.createMetadata(Fn.class, "foo");
    FunctionModelConfig config = config(implementations(TestFn.class, BasicImpl.class));
    FunctionModelNode node = FunctionModelNode.create(TestFn.class, config, Collections.<Class<?>>emptySet(), NodeDecorator.IDENTITY);
    FunctionModel.forFunction(metadata, node);
  }

  @Test
  public void invalidFunctionImplementation() {
    Map<Class<?>, Class<?>> impls =  ImmutableMap.<Class<?>, Class<?>>of(Fn.class, Object.class);
    FunctionModelConfig config = new FunctionModelConfig(impls);
    FunctionMetadata metadata = EngineUtils.createMetadata(Fn.class, "foo");
    FunctionModel model = FunctionModel.forFunction(metadata, config);
    assertFalse(model.isValid());
    assertTrue(model.getRoot() instanceof ErrorNode);
    assertFalse(model.getRoot().getExceptions().isEmpty());
    assertTrue(model.getRoot().getExceptions().get(0) instanceof InvalidImplementationException);
  }

  public interface Fn {

    @Output("Foo")
    String foo(Integer d);
  }

  public static class Impl implements Fn {

    @Override
    public String foo(Integer d) {
      return d.toString();
    }
  }

  public static class Decorator1 implements Fn, ScenarioFunction<Arg1, Decorator1> {

    private final Fn _delegate;

    public Decorator1(Fn delegate) {
      _delegate = delegate;
    }

    @Override
    public String foo(Integer d) {
      return _delegate.foo(2 * d);
    }

    @Nullable
    @Override
    public Class<Arg1> getArgumentType() {
      return Arg1.class;
    }
  }

  public static class Arg1 implements ScenarioArgument<Arg1, Decorator1> {

    @Override
    public Class<Decorator1> getFunctionType() {
      return Decorator1.class;
    }
  }

  public static class Decorator2 implements Fn, ScenarioFunction<Arg2, Decorator2> {

    private final Fn _delegate;

    public Decorator2(Fn fn) {
      _delegate = fn;
    }

    @Override
    public String foo(Integer d) {
      return _delegate.foo(1 + d);
    }

    @Nullable
    @Override
    public Class<Arg2> getArgumentType() {
      return Arg2.class;
    }
  }

  public static class Arg2 implements ScenarioArgument<Arg2, Decorator2> {

    @Override
    public Class<Decorator2> getFunctionType() {
      return Decorator2.class;
    }
  }
}

/* package */ interface TestFn {

  @Output("Foo")
  Object foo();
}

/* package */ class BasicImpl implements TestFn {

  public BasicImpl() {
  }

  @Override
  public Object foo() {
    return "foo";
  }
}

/* package */ class InfrastructureImpl implements TestFn {

  /* package */ final String _infrastructureComponent;

  public InfrastructureImpl(String infrastructureComponent) {
    _infrastructureComponent = infrastructureComponent;
  }

  @Override
  public Object foo() {
    return null;
  }
}

/* package */ class CallsOtherFn implements TestFn {

  /* package */ final CollaboratorFn _collaborator;

  public CallsOtherFn(CollaboratorFn collaborator) {
    _collaborator = collaborator;
  }

  @Override
  public Object foo() {
    return null;
  }
}

/* package */ interface CollaboratorFn { }

/* package */ class Collaborator implements CollaboratorFn {

  public Collaborator() {
  }
}

/* package */ class BrokenCollaborator implements CollaboratorFn {

  public BrokenCollaborator(Object unsatisfiedArg) {
  }
}

/* package */ class Concrete1 {

  /* package */ final Concrete2 _concrete;

  public Concrete1(Concrete2 concrete) {
    _concrete = concrete;
  }

  @Output("Foo")
  public Object foo() {
    return null;
  }
}
/* package */ class Concrete2 {

  public Concrete2() {
  }
}

/**
 * A class with a private constructor that can only be created via a factory method. Need to use a provider to build.
 */
/* package */ class PrivateConstructor {

  private final String _name;

  private PrivateConstructor(String name) {
    _name = name;
  }

  /* package */ static PrivateConstructor build(String name) {
    return new PrivateConstructor(name);
  }

  @Output("Name")
  public String getName() {
    return _name;
  }
}

/**
 * A provider that creates a class using a factory method. Has injectable parameters of its own.
 */
/* package */ class PrivateConstructorProvider implements Provider<PrivateConstructor> {

  private final String _providerName;

  public PrivateConstructorProvider(String providerName) {
    _providerName = providerName;
  }

  @Override
  public PrivateConstructor get() {
    return PrivateConstructor.build(_providerName);
  }
}

/* package */ class NoSuitableConstructor {

  public NoSuitableConstructor() {
  }

  public NoSuitableConstructor(String ignored) {
  }

  @Output("Foo")
  public Object foo() {
    return null;
  }

}
