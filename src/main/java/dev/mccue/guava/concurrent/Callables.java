/*
 * Copyright (C) 2009 The Guava Authors
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

import static dev.mccue.guava.base.Preconditions.checkNotNull;

import dev.mccue.guava.base.Supplier;
import java.util.concurrent.Callable;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Static utility methods pertaining to the {@code Callable} interface.
 *
 * @author Isaac Shum
 * @since 1.0
 */

@ElementTypesAreNonnullByDefault
public final class Callables {
  private Callables() {}

  /** Creates a {@code Callable} which immediately returns a preset value each time it is called. */
  public static <T extends @Nullable Object> Callable<T> returning(@ParametricNullness T value) {
    return () -> value;
  }

  /**
   * Creates an {@code AsyncCallable} from a {@code Callable}.
   *
   * <p>The {@code AsyncCallable} returns the {@code ListenableFuture} resulting from {@code
   * ListeningExecutorService#submit(Callable)}.
   *
   * @since 20.0
   */
  public static <T extends @Nullable Object> AsyncCallable<T> asAsyncCallable(
      Callable<T> callable, ListeningExecutorService listeningExecutorService) {
    checkNotNull(callable);
    checkNotNull(listeningExecutorService);
    return () -> listeningExecutorService.submit(callable);
  }

  /**
   * Wraps the given callable such that for the duration of {@code Callable#call} the thread that is
   * running will have the given name.
   *
   * @param callable The callable to wrap
   * @param nameSupplier The supplier of thread names, {@code Supplier#get get} will be called once
   *     for each invocation of the wrapped callable.
   */
  // threads
  static <T extends @Nullable Object> Callable<T> threadRenaming(
      Callable<T> callable, Supplier<String> nameSupplier) {
    checkNotNull(nameSupplier);
    checkNotNull(callable);
    return () -> {
      Thread currentThread = Thread.currentThread();
      String oldName = currentThread.getName();
      boolean restoreName = trySetName(nameSupplier.get(), currentThread);
      try {
        return callable.call();
      } finally {
        if (restoreName) {
          boolean unused = trySetName(oldName, currentThread);
        }
      }
    };
  }

  /**
   * Wraps the given runnable such that for the duration of {@code Runnable#run} the thread that is
   * running with have the given name.
   *
   * @param task The Runnable to wrap
   * @param nameSupplier The supplier of thread names, {@code Supplier#get get} will be called once
   *     for each invocation of the wrapped callable.
   */
  // threads
  static Runnable threadRenaming(Runnable task, Supplier<String> nameSupplier) {
    checkNotNull(nameSupplier);
    checkNotNull(task);
    return () -> {
      Thread currentThread = Thread.currentThread();
      String oldName = currentThread.getName();
      boolean restoreName = trySetName(nameSupplier.get(), currentThread);
      try {
        task.run();
      } finally {
        if (restoreName) {
          boolean unused = trySetName(oldName, currentThread);
        }
      }
    };
  }

  /** Tries to set name of the given {@code Thread}, returns true if successful. */
  // threads
  private static boolean trySetName(String threadName, Thread currentThread) {
    /*
     * setName should usually succeed, but the security manager can prohibit it. Is there a way to
     * see if we have the modifyThread permission without catching an exception?
     */
    try {
      currentThread.setName(threadName);
      return true;
    } catch (SecurityException e) {
      return false;
    }
  }
}
