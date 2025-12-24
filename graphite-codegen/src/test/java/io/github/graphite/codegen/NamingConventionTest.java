/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.graphite.codegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("NamingConvention")
class NamingConventionTest {

  @Nested
  @DisplayName("defaults()")
  class Defaults {

    private final NamingConvention convention = NamingConvention.defaults();

    @Test
    @DisplayName("should return singleton instance")
    void shouldReturnSingletonInstance() {
      NamingConvention first = NamingConvention.defaults();
      NamingConvention second = NamingConvention.defaults();

      assertThat(first).isSameAs(second);
    }

    @Test
    @DisplayName("should add DTO suffix to types")
    void shouldAddDtoSuffixToTypes() {
      assertThat(convention.getTypeName("User")).isEqualTo("UserDTO");
      assertThat(convention.getTypeName("order")).isEqualTo("OrderDTO");
    }

    @Test
    @DisplayName("should add Input suffix to input types")
    void shouldAddInputSuffixToInputTypes() {
      assertThat(convention.getInputTypeName("CreateUser")).isEqualTo("CreateUserInput");
    }

    @Test
    @DisplayName("should not double-add Input suffix")
    void shouldNotDoubleAddInputSuffix() {
      assertThat(convention.getInputTypeName("CreateUserInput")).isEqualTo("CreateUserInput");
    }

    @Test
    @DisplayName("should add Query suffix to queries")
    void shouldAddQuerySuffixToQueries() {
      assertThat(convention.getQueryName("getUser")).isEqualTo("GetUserQuery");
      assertThat(convention.getQueryName("listOrders")).isEqualTo("ListOrdersQuery");
    }

    @Test
    @DisplayName("should not double-add Query suffix")
    void shouldNotDoubleAddQuerySuffix() {
      assertThat(convention.getQueryName("GetUserQuery")).isEqualTo("GetUserQuery");
    }

    @Test
    @DisplayName("should add Mutation suffix to mutations")
    void shouldAddMutationSuffixToMutations() {
      assertThat(convention.getMutationName("createUser")).isEqualTo("CreateUserMutation");
    }

    @Test
    @DisplayName("should not double-add Mutation suffix")
    void shouldNotDoubleAddMutationSuffix() {
      assertThat(convention.getMutationName("CreateUserMutation")).isEqualTo("CreateUserMutation");
    }

    @Test
    @DisplayName("should capitalize enum names")
    void shouldCapitalizeEnumNames() {
      assertThat(convention.getEnumName("userStatus")).isEqualTo("UserStatus");
      assertThat(convention.getEnumName("OrderState")).isEqualTo("OrderState");
    }

    @Test
    @DisplayName("should capitalize interface names")
    void shouldCapitalizeInterfaceNames() {
      assertThat(convention.getInterfaceName("node")).isEqualTo("Node");
      assertThat(convention.getInterfaceName("Timestamped")).isEqualTo("Timestamped");
    }

    @Test
    @DisplayName("should capitalize union names")
    void shouldCapitalizeUnionNames() {
      assertThat(convention.getUnionName("searchResult")).isEqualTo("SearchResult");
    }

    @Test
    @DisplayName("should add Projection suffix")
    void shouldAddProjectionSuffix() {
      assertThat(convention.getProjectionName("User")).isEqualTo("UserProjection");
      assertThat(convention.getProjectionName("order")).isEqualTo("OrderProjection");
    }

    @Test
    @DisplayName("should handle empty string")
    void shouldHandleEmptyString() {
      assertThat(convention.getTypeName("")).isEqualTo("DTO");
      assertThat(convention.getEnumName("")).isEmpty();
    }
  }

  @Nested
  @DisplayName("withSuffixes()")
  class WithSuffixes {

    @Test
    @DisplayName("should use custom suffixes")
    void shouldUseCustomSuffixes() {
      NamingConvention convention =
          NamingConvention.withSuffixes("Model", "Request", "Fetch", "Action");

      assertThat(convention.getTypeName("User")).isEqualTo("UserModel");
      assertThat(convention.getInputTypeName("Create")).isEqualTo("CreateRequest");
      assertThat(convention.getQueryName("getUser")).isEqualTo("GetUserFetch");
      assertThat(convention.getMutationName("create")).isEqualTo("CreateAction");
    }

    @Test
    @DisplayName("should throw on null typeSuffix")
    void shouldThrowOnNullTypeSuffix() {
      assertThatThrownBy(() -> NamingConvention.withSuffixes(null, "Input", "Query", "Mutation"))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("typeSuffix");
    }

    @Test
    @DisplayName("should throw on null inputSuffix")
    void shouldThrowOnNullInputSuffix() {
      assertThatThrownBy(() -> NamingConvention.withSuffixes("DTO", null, "Query", "Mutation"))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("inputSuffix");
    }

    @Test
    @DisplayName("should throw on null querySuffix")
    void shouldThrowOnNullQuerySuffix() {
      assertThatThrownBy(() -> NamingConvention.withSuffixes("DTO", "Input", null, "Mutation"))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("querySuffix");
    }

    @Test
    @DisplayName("should throw on null mutationSuffix")
    void shouldThrowOnNullMutationSuffix() {
      assertThatThrownBy(() -> NamingConvention.withSuffixes("DTO", "Input", "Query", null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("mutationSuffix");
    }
  }

  @Nested
  @DisplayName("null handling")
  class NullHandling {

    private final NamingConvention convention = NamingConvention.defaults();

    @Test
    @DisplayName("getTypeName should throw on null")
    void getTypeNameShouldThrowOnNull() {
      assertThatThrownBy(() -> convention.getTypeName(null))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("getInputTypeName should throw on null")
    void getInputTypeNameShouldThrowOnNull() {
      assertThatThrownBy(() -> convention.getInputTypeName(null))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("getQueryName should throw on null")
    void getQueryNameShouldThrowOnNull() {
      assertThatThrownBy(() -> convention.getQueryName(null))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("getMutationName should throw on null")
    void getMutationNameShouldThrowOnNull() {
      assertThatThrownBy(() -> convention.getMutationName(null))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("getEnumName should throw on null")
    void getEnumNameShouldThrowOnNull() {
      assertThatThrownBy(() -> convention.getEnumName(null))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("getInterfaceName should throw on null")
    void getInterfaceNameShouldThrowOnNull() {
      assertThatThrownBy(() -> convention.getInterfaceName(null))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("getUnionName should throw on null")
    void getUnionNameShouldThrowOnNull() {
      assertThatThrownBy(() -> convention.getUnionName(null))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("getProjectionName should throw on null")
    void getProjectionNameShouldThrowOnNull() {
      assertThatThrownBy(() -> convention.getProjectionName(null))
          .isInstanceOf(NullPointerException.class);
    }
  }
}
