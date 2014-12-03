// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import com.google.auto.value.AutoValue;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.squareup.protoparser.Utils.appendDocumentation;
import static com.squareup.protoparser.Utils.appendIndented;
import static com.squareup.protoparser.Utils.checkNotNull;
import static com.squareup.protoparser.Utils.immutableCopyOf;

/** An enumerated type declaration. */
@AutoValue
public abstract class EnumElement implements TypeElement {
  private static void validateTagUniqueness(String qualifiedName,
      List<EnumConstantElement> constants) {
    checkNotNull(qualifiedName, "qualifiedName");

    Set<Integer> tags = new LinkedHashSet<>();
    for (EnumConstantElement constant : constants) {
      int tag = constant.tag();
      if (!tags.add(tag)) {
        throw new IllegalStateException("Duplicate tag " + tag + " in " + qualifiedName);
      }
    }
  }

  private static boolean parseAllowAlias(List<OptionElement> options) {
    OptionElement option = OptionElement.findByName(options, "allow_alias");
    if (option != null) {
      Object value = option.value();
      return value instanceof Boolean && (Boolean) value;
    }
    return false;
  }

  /**
   * Though not mentioned in the spec, enum names use C++ scoping rules, meaning that enum constants
   * are siblings of their declaring element, not children of it.
   */
  static void validateValueUniquenessInScope(String qualifiedName,
      List<TypeElement> nestedElements) {
    checkNotNull(qualifiedName, "qualifiedName");

    Set<String> names = new LinkedHashSet<>();
    for (TypeElement nestedElement : nestedElements) {
      if (nestedElement instanceof EnumElement) {
        EnumElement enumElement = (EnumElement) nestedElement;
        for (EnumConstantElement constant : enumElement.constants()) {
          String name = constant.name();
          if (!names.add(name)) {
            throw new IllegalStateException(
                "Duplicate enum constant " + name + " in scope " + qualifiedName);
          }
        }
      }
    }
  }

  public static EnumElement create(String name, String qualifiedName, String documentation,
      List<OptionElement> options, List<EnumConstantElement> constants) {
    if (!parseAllowAlias(options)) {
      validateTagUniqueness(qualifiedName, constants);
    }

    return new AutoValue_EnumElement(name, qualifiedName, documentation,
        immutableCopyOf(constants, "constants"), immutableCopyOf(options, "options"));
  }

  EnumElement() {
  }

  @Override public abstract String name();
  @Override public abstract String qualifiedName();
  @Override public abstract String documentation();
  public abstract List<EnumConstantElement> constants();
  @Override public abstract List<OptionElement> options();

  @Override public final List<TypeElement> nestedElements() {
    return Collections.emptyList(); // Enums do not allow nested type declarations.
  }

  @Override public final String toString() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation());
    builder.append("enum ")
        .append(name())
        .append(" {");
    if (!options().isEmpty()) {
      builder.append('\n');
      for (OptionElement option : options()) {
        appendIndented(builder, option.toDeclaration());
      }
    }
    if (!constants().isEmpty()) {
      builder.append('\n');
      for (EnumConstantElement constant : constants()) {
        appendIndented(builder, constant.toString());
      }
    }
    return builder.append("}\n").toString();
  }
}
