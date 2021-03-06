/*-
 * -\-\-
 * protoman-validation
 * --
 * Copyright (C) 2016 - 2018 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */

package com.spotify.protoman.validation.rules;

import static com.spotify.protoman.validation.ValidationViolationMatcher.validationViolation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableList;
import com.spotify.protoman.descriptor.DescriptorSet;
import com.spotify.protoman.testutil.DescriptorSetUtils;
import com.spotify.protoman.validation.DefaultSchemaValidator;
import com.spotify.protoman.validation.SchemaValidator;
import com.spotify.protoman.validation.ValidationViolation;
import com.spotify.protoman.validation.ViolationType;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class MethodInputTypeCompatibilityRuleTest {

  private final SchemaValidator schemaValidator = DefaultSchemaValidator.builder()
      .addRule(MethodInputTypeCompatibilityRule.create())
      .build();

  private static final String TEMPLATE =
      "syntax = 'proto3';\n"
      + "service Derp {\n"
      + "  rpc GetDerp (%s) returns (Empty);\n"
      + "}\n"
      + "message Empty {}\n"
      + "message A {\n"
      + "  int32 a = 1;\n"
      + "}\n"
      + "message SuperTypeOfA {\n"
      + "  int32 a = 1;\n"
      + "  string b = 2;\n"
      + "}\n"
      + "message SameAsA {\n"
      + "  int32 a = 1;\n"
      + "}\n"
      + "message WireCompatWithA {\n"
      + "  bool a = 1;\n"
      + "}\n"
      + "message WireIncompatWithA {\n"
      + "  int32 abc = 2;\n"
      + "}\n"
      + "message WireCompatFieldNameChanged {\n"
      + "  int32 a_different_name = 1;\n"
      + "}\n";

  @Parameters(method = "testCases")
  @Test
  public void testInputTypeChanged(
      final String currentType, final String candidateType,
      final ViolationType expectedViolationType,
      final String expectedDescription) throws Exception {
    final DescriptorSet current = DescriptorSetUtils.buildDescriptorSet(
        "a.proto", String.format(TEMPLATE, currentType)
    );
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto", String.format(TEMPLATE, candidateType)
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(
        violations,
        contains(
            validationViolation()
                .description(equalTo(expectedDescription))
                .type(equalTo(expectedViolationType))
        )
    );
  }

  private static Object[] testCases() {
    return new Object[][]{
        new Object[]{
            "A", "SameAsA",
            ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
            "method input type changed"
        },
        new Object[]{
            "A", "WireCompatWithA",
            ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
            "method input type changed"
        },
        new Object[]{
            "A", "SuperTypeOfA",
            ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
            "method input type changed"
        },
        new Object[]{
            "A", "Empty",
            ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
            "input type changed: message types A and Empty are not interchangable, field a does "
            + "exist in the new message type used"
        },
        new Object[]{
            "A", "WireIncompatWithA",
            ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
            "input type changed: message types A and WireIncompatWithA are not interchangable, "
            + "field a does exist in the new message type used"
        },
        new Object[]{
            "A", "WireCompatFieldNameChanged",
            ViolationType.FIELD_MASK_INCOMPATIBILITY,
            "input type changed: message types A and WireCompatFieldNameChanged are not "
            + "interchangable, field a has different name in new message type used (current=a, "
            + "candidate=a_different_name)"
        },
    };
  }
}