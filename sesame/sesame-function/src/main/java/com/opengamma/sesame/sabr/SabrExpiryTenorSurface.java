/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.sabr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.opengamma.core.marketdatasnapshot.NamedSnapshot;
import com.opengamma.id.UniqueId;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

/**
 * Joda-bean holding the raw data for a SABR surface. Other areas
 * of config will be responsible for the interpolation to be used,
 * which means the same data can be used with multiple interpolators.
 *
 * The data forms a surface (i.e. we have a z-value for a pair
 * of x and y values). However, although the x and y values will be
 * regular there may be combinations that are missing. For this reason,
 * we store the data as a list of points (x, y, z) rather than any
 * form of 2D array.
 */
@BeanDefinition
public final class SabrExpiryTenorSurface implements ImmutableBean, NamedSnapshot {

  /**
   * The unique id of the surface, null if it has not been added to a master.
   * Not null after insertion/retrieval from master.
   */
  @PropertyDefinition
  private final UniqueId _uniqueId;

  /**
   * The name for this surface.
   */
  @PropertyDefinition(validate = "notNull")
  private final String _name;

  /**
   * The points forming the surface. There must be at least one point
   * specified. Sorted so that viewing in a UI is straightforward and
   * creating analytics objects is straightforward.
   */
  @PropertyDefinition(validate = "notNull")
  private final List<SabrNode> _sabrData;

  /**
   * Create the surface, ensuring that the nodes held are sorted.
   * Checks are done to ensure that the SabrNodes are unique
   * i.e. for an x/y combination there is only one z-value.
   *
   * @param name  the name for the surface, not null or empty
   * @param sabrData  the data for the surface, not null or empty.
   */
  public SabrExpiryTenorSurface(String name, List<SabrNode> sabrData) {
    this(null, name, createSortedNodes(ArgumentChecker.notNull(sabrData, "sabrData")));
  }

  /**
   * Create the surface. This method is used internally for
   * creating the surface when it is deserialized. As the
   * surface would have been sorted before serialization
   * there is no need to sort again here.
   *
   * @param name  the name for the surface, not null or empty
   * @param sabrData  the data for the surface, not null or empty.
   */
  @ImmutableConstructor
  private SabrExpiryTenorSurface(UniqueId uniqueId, String name, List<SabrNode> sabrData) {
    _uniqueId = uniqueId;
    _name = ArgumentChecker.notEmpty(name, "name");
    _sabrData = ArgumentChecker.notEmpty(sabrData, "sabrData");
  }

  /**
   * Create a copy of this surface with the unique id set to the
   * supplied value. This is intended for use by masters when
   * inserting new items.
   *
   * @param uniqueId  the new value for the unique id, not null
   * @return a copy of this surface, not null
   */
  @Override
  public SabrExpiryTenorSurface withUniqueId(UniqueId uniqueId) {
    return new SabrExpiryTenorSurface(ArgumentChecker.notNull(uniqueId, "uniqueId"), _name, _sabrData);
  }

  private static ImmutableList<SabrNode> createSortedNodes(List<SabrNode> sabrData) {
    // By using a sorted map we can handle uniqueness of the points
    // and sorting the data in one place
    Map<Pair<Double, Double>, SabrNode> sorted = new TreeMap<>();
    for (SabrNode node : sabrData) {
      Pair<Double, Double> key = Pairs.of(node.getX(), node.getY());
      if (!sorted.containsKey(key)) {
        sorted.put(key, node);
      } else {
        throw new IllegalArgumentException(
            "Duplicate surface node with x: " + node.getX() + " and y: " + node.getY());
      }
    }
    return ImmutableList.copyOf(sorted.values());
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SabrExpiryTenorSurface}.
   * @return the meta-bean, not null
   */
  public static SabrExpiryTenorSurface.Meta meta() {
    return SabrExpiryTenorSurface.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SabrExpiryTenorSurface.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static SabrExpiryTenorSurface.Builder builder() {
    return new SabrExpiryTenorSurface.Builder();
  }

  @Override
  public SabrExpiryTenorSurface.Meta metaBean() {
    return SabrExpiryTenorSurface.Meta.INSTANCE;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the unique id of the surface, null if it has not been added to a master.
   * Not null after insertion/retrieval from master.
   * @return the value of the property
   */
  public UniqueId getUniqueId() {
    return _uniqueId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the name for this surface.
   * @return the value of the property, not null
   */
  public String getName() {
    return _name;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the points forming the surface. There must be at least one point
   * specified. Sorted so that viewing in a UI is straightforward and
   * creating analytics objects is straightforward.
   * @return the value of the property, not null
   */
  public List<SabrNode> getSabrData() {
    return _sabrData;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SabrExpiryTenorSurface other = (SabrExpiryTenorSurface) obj;
      return JodaBeanUtils.equal(getUniqueId(), other.getUniqueId()) &&
          JodaBeanUtils.equal(getName(), other.getName()) &&
          JodaBeanUtils.equal(getSabrData(), other.getSabrData());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getUniqueId());
    hash = hash * 31 + JodaBeanUtils.hashCode(getName());
    hash = hash * 31 + JodaBeanUtils.hashCode(getSabrData());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("SabrExpiryTenorSurface{");
    buf.append("uniqueId").append('=').append(getUniqueId()).append(',').append(' ');
    buf.append("name").append('=').append(getName()).append(',').append(' ');
    buf.append("sabrData").append('=').append(JodaBeanUtils.toString(getSabrData()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SabrExpiryTenorSurface}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code uniqueId} property.
     */
    private final MetaProperty<UniqueId> _uniqueId = DirectMetaProperty.ofImmutable(
        this, "uniqueId", SabrExpiryTenorSurface.class, UniqueId.class);
    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<String> _name = DirectMetaProperty.ofImmutable(
        this, "name", SabrExpiryTenorSurface.class, String.class);
    /**
     * The meta-property for the {@code sabrData} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<SabrNode>> _sabrData = DirectMetaProperty.ofImmutable(
        this, "sabrData", SabrExpiryTenorSurface.class, (Class) List.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "uniqueId",
        "name",
        "sabrData");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -294460212:  // uniqueId
          return _uniqueId;
        case 3373707:  // name
          return _name;
        case 1661297640:  // sabrData
          return _sabrData;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public SabrExpiryTenorSurface.Builder builder() {
      return new SabrExpiryTenorSurface.Builder();
    }

    @Override
    public Class<? extends SabrExpiryTenorSurface> beanType() {
      return SabrExpiryTenorSurface.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return _metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code uniqueId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<UniqueId> uniqueId() {
      return _uniqueId;
    }

    /**
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> name() {
      return _name;
    }

    /**
     * The meta-property for the {@code sabrData} property.
     * @return the meta-property, not null
     */
    public MetaProperty<List<SabrNode>> sabrData() {
      return _sabrData;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -294460212:  // uniqueId
          return ((SabrExpiryTenorSurface) bean).getUniqueId();
        case 3373707:  // name
          return ((SabrExpiryTenorSurface) bean).getName();
        case 1661297640:  // sabrData
          return ((SabrExpiryTenorSurface) bean).getSabrData();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code SabrExpiryTenorSurface}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<SabrExpiryTenorSurface> {

    private UniqueId _uniqueId;
    private String _name;
    private List<SabrNode> _sabrData = new ArrayList<SabrNode>();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(SabrExpiryTenorSurface beanToCopy) {
      this._uniqueId = beanToCopy.getUniqueId();
      this._name = beanToCopy.getName();
      this._sabrData = new ArrayList<SabrNode>(beanToCopy.getSabrData());
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -294460212:  // uniqueId
          return _uniqueId;
        case 3373707:  // name
          return _name;
        case 1661297640:  // sabrData
          return _sabrData;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -294460212:  // uniqueId
          this._uniqueId = (UniqueId) newValue;
          break;
        case 3373707:  // name
          this._name = (String) newValue;
          break;
        case 1661297640:  // sabrData
          this._sabrData = (List<SabrNode>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public SabrExpiryTenorSurface build() {
      return new SabrExpiryTenorSurface(
          _uniqueId,
          _name,
          _sabrData);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code uniqueId} property in the builder.
     * @param uniqueId  the new value
     * @return this, for chaining, not null
     */
    public Builder uniqueId(UniqueId uniqueId) {
      this._uniqueId = uniqueId;
      return this;
    }

    /**
     * Sets the {@code name} property in the builder.
     * @param name  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder name(String name) {
      JodaBeanUtils.notNull(name, "name");
      this._name = name;
      return this;
    }

    /**
     * Sets the {@code sabrData} property in the builder.
     * @param sabrData  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder sabrData(List<SabrNode> sabrData) {
      JodaBeanUtils.notNull(sabrData, "sabrData");
      this._sabrData = sabrData;
      return this;
    }

    /**
     * Sets the {@code sabrData} property in the builder
     * from an array of objects.
     * @param sabrData  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder sabrData(SabrNode... sabrData) {
      return sabrData(Arrays.asList(sabrData));
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("SabrExpiryTenorSurface.Builder{");
      buf.append("uniqueId").append('=').append(JodaBeanUtils.toString(_uniqueId)).append(',').append(' ');
      buf.append("name").append('=').append(JodaBeanUtils.toString(_name)).append(',').append(' ');
      buf.append("sabrData").append('=').append(JodaBeanUtils.toString(_sabrData));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
