/*
 * Copyright (C) 2019 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package dev.mccue.guava.concurrent;

import java.time.Duration;

/** This class is for {@code dev.mccue.guava.concurrent} use only! */
// java.time.Duration
@ElementTypesAreNonnullByDefault
final class Internal {

  /**
   * Returns the number of nanoseconds of the given duration without throwing or overflowing.
   *
   * <p>Instead of throwing {@code ArithmeticException}, this method silently saturates to either
   * {@code Long#MAX_VALUE} or {@code Long#MIN_VALUE}. This behavior can be useful when decomposing
   * a duration in order to call a legacy API which requires a {@code long, TimeUnit} pair.
   */
  static long toNanosSaturated(Duration duration) {
    // Using a try/catch seems lazy, but the catch block will rarely get invoked (except for
    // durations longer than approximately +/- 292 years).
    try {
      return duration.toNanos();
    } catch (ArithmeticException tooBig) {
      return duration.isNegative() ? Long.MIN_VALUE : Long.MAX_VALUE;
    }
  }

  private Internal() {}
}
