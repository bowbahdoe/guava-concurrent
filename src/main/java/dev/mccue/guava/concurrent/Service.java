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

import static dev.mccue.guava.concurrent.Internal.toNanosSaturated;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.DoNotMock;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An object with an operational state, plus asynchronous {@code #startAsync()} and {@code
 * #stopAsync()} lifecycle methods to transition between states. Example services include
 * webservers, RPC servers and timers.
 *
 * <p>The normal lifecycle of a service is:
 *
 * <ul>
 *   <li>{@code State#NEW NEW} -&gt;
 *   <li>{@code State#STARTING STARTING} -&gt;
 *   <li>{@code State#RUNNING RUNNING} -&gt;
 *   <li>{@code State#STOPPING STOPPING} -&gt;
 *   <li>{@code State#TERMINATED TERMINATED}
 * </ul>
 *
 * <p>There are deviations from this if there are failures or if {@code Service#stopAsync} is called
 * before the {@code Service} reaches the {@code State#RUNNING RUNNING} state. The set of legal
 * transitions form a <a href="http://en.wikipedia.org/wiki/Directed_acyclic_graph">DAG</a>,
 * therefore every method of the listener will be called at most once. N.B. The {@code State#FAILED}
 * and {@code State#TERMINATED} states are terminal states, once a service enters either of these
 * states it cannot ever leave them.
 *
 * <p>Implementors of this interface are strongly encouraged to extend one of the abstract classes
 * in this package which implement this interface and make the threading and state management
 * easier.
 *
 * @author Jesse Wilson
 * @author Luke Sandberg
 * @since 9.0 (in 1.0 as {@code dev.mccue.guava.base.Service})
 */
@DoNotMock("Create an AbstractIdleService")
@ElementTypesAreNonnullByDefault
public interface Service {
  /**
   * If the service state is {@code State#NEW}, this initiates service startup and returns
   * immediately. A stopped service may not be restarted.
   *
   * @return this
   * @throws IllegalStateException if the service is not {@code State#NEW}
   * @since 15.0
   */
  @CanIgnoreReturnValue
  Service startAsync();

  /** Returns {@code true} if this service is {@code State#RUNNING running}. */
  boolean isRunning();

  /** Returns the lifecycle state of the service. */
  State state();

  /**
   * If the service is {@code State#STARTING starting} or {@code State#RUNNING running},
   * this initiates service shutdown and returns immediately. If the service is {@code
   * State#NEW new}, it is {@code State#TERMINATED terminated} without having been started nor
   * stopped. If the service has already been stopped, this method returns immediately without
   * taking action.
   *
   * @return this
   * @since 15.0
   */
  @CanIgnoreReturnValue
  Service stopAsync();

  /**
   * Waits for the {@code Service} to reach the {@code State#RUNNING running state}.
   *
   * @throws IllegalStateException if the service reaches a state from which it is not possible to
   *     enter the {@code State#RUNNING} state. e.g. if the {@code state} is {@code
   *     State#TERMINATED} when this method is called then this will throw an IllegalStateException.
   * @since 15.0
   */
  void awaitRunning();

  /**
   * Waits for the {@code Service} to reach the {@code State#RUNNING running state} for no more
   * than the given time.
   *
   * @param timeout the maximum time to wait
   * @throws TimeoutException if the service has not reached the given state within the deadline
   * @throws IllegalStateException if the service reaches a state from which it is not possible to
   *     enter the {@code State#RUNNING RUNNING} state. e.g. if the {@code state} is {@code
   *     State#TERMINATED} when this method is called then this will throw an IllegalStateException.
   * @since 28.0
   */
  default void awaitRunning(Duration timeout) throws TimeoutException {
    awaitRunning(toNanosSaturated(timeout), TimeUnit.NANOSECONDS);
  }

  /**
   * Waits for the {@code Service} to reach the {@code State#RUNNING running state} for no more
   * than the given time.
   *
   * @param timeout the maximum time to wait
   * @param unit the time unit of the timeout argument
   * @throws TimeoutException if the service has not reached the given state within the deadline
   * @throws IllegalStateException if the service reaches a state from which it is not possible to
   *     enter the {@code State#RUNNING RUNNING} state. e.g. if the {@code state} is {@code
   *     State#TERMINATED} when this method is called then this will throw an IllegalStateException.
   * @since 15.0
   */
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  void awaitRunning(long timeout, TimeUnit unit) throws TimeoutException;

  /**
   * Waits for the {@code Service} to reach the {@code State#TERMINATED terminated state}.
   *
   * @throws IllegalStateException if the service {@code State#FAILED fails}.
   * @since 15.0
   */
  void awaitTerminated();

  /**
   * Waits for the {@code Service} to reach a terminal state (either {@code Service.State#TERMINATED
   * terminated} or {@code Service.State#FAILED failed}) for no more than the given time.
   *
   * @param timeout the maximum time to wait
   * @throws TimeoutException if the service has not reached the given state within the deadline
   * @throws IllegalStateException if the service {@code State#FAILED fails}.
   * @since 28.0
   */
  default void awaitTerminated(Duration timeout) throws TimeoutException {
    awaitTerminated(toNanosSaturated(timeout), TimeUnit.NANOSECONDS);
  }

  /**
   * Waits for the {@code Service} to reach a terminal state (either {@code Service.State#TERMINATED
   * terminated} or {@code Service.State#FAILED failed}) for no more than the given time.
   *
   * @param timeout the maximum time to wait
   * @param unit the time unit of the timeout argument
   * @throws TimeoutException if the service has not reached the given state within the deadline
   * @throws IllegalStateException if the service {@code State#FAILED fails}.
   * @since 15.0
   */
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  void awaitTerminated(long timeout, TimeUnit unit) throws TimeoutException;

  /**
   * Returns the {@code Throwable} that caused this service to fail.
   *
   * @throws IllegalStateException if this service's state isn't {@code State#FAILED FAILED}.
   * @since 14.0
   */
  Throwable failureCause();

  /**
   * Registers a {@code Listener} to be {@code Executor#execute executed} on the given
   * executor. The listener will have the corresponding transition method called whenever the
   * service changes state. The listener will not have previous state changes replayed, so it is
   * suggested that listeners are added before the service starts.
   *
   * <p>{@code addListener} guarantees execution ordering across calls to a given listener but not
   * across calls to multiple listeners. Specifically, a given listener will have its callbacks
   * invoked in the same order as the underlying service enters those states. Additionally, at most
   * one of the listener's callbacks will execute at once. However, multiple listeners' callbacks
   * may execute concurrently, and listeners may execute in an order different from the one in which
   * they were registered.
   *
   * <p>RuntimeExceptions thrown by a listener will be caught and logged. Any exception thrown
   * during {@code Executor.execute} (e.g., a {@code RejectedExecutionException}) will be caught and
   * logged.
   *
   * @param listener the listener to run when the service changes state is complete
   * @param executor the executor in which the listeners callback methods will be run. For fast,
   *     lightweight listeners that would be safe to execute in any thread, consider {@code
   *     MoreExecutors#directExecutor}.
   * @since 13.0
   */
  void addListener(Listener listener, Executor executor);

  /**
   * The lifecycle states of a service.
   *
   * <p>The ordering of the {@code State} enum is defined such that if there is a state transition
   * from {@code A -> B} then {@code A.compareTo(B) < 0}. N.B. The converse is not true, i.e. if
   * {@code A.compareTo(B) < 0} then there is <b>not</b> guaranteed to be a valid state transition
   * {@code A -> B}.
   *
   * @since 9.0 (in 1.0 as {@code dev.mccue.guava.base.Service.State})
   */
  enum State {
    /** A service in this state is inactive. It does minimal work and consumes minimal resources. */
    NEW,

    /** A service in this state is transitioning to {@code #RUNNING}. */
    STARTING,

    /** A service in this state is operational. */
    RUNNING,

    /** A service in this state is transitioning to {@code #TERMINATED}. */
    STOPPING,

    /**
     * A service in this state has completed execution normally. It does minimal work and consumes
     * minimal resources.
     */
    TERMINATED,

    /**
     * A service in this state has encountered a problem and may not be operational. It cannot be
     * started nor stopped.
     */
    FAILED,
  }

  /**
   * A listener for the various state changes that a {@code Service} goes through in its lifecycle.
   *
   * <p>All methods are no-ops by default, implementors should override the ones they care about.
   *
   * @author Luke Sandberg
   * @since 15.0 (present as an interface in 13.0)
   */
  abstract class Listener {
    /** Constructor for use by subclasses. */
    public Listener() {}

    /**
     * Called when the service transitions from {@code State#NEW NEW} to {@code
     * State#STARTING STARTING}. This occurs when {@code Service#startAsync} is called the first
     * time.
     */
    public void starting() {}

    /**
     * Called when the service transitions from {@code State#STARTING STARTING} to {@code
     * State#RUNNING RUNNING}. This occurs when a service has successfully started.
     */
    public void running() {}

    /**
     * Called when the service transitions to the {@code State#STOPPING STOPPING} state. The
     * only valid values for {@code from} are {@code State#STARTING STARTING} or {@code
     * State#RUNNING RUNNING}. This occurs when {@code Service#stopAsync} is called.
     *
     * @param from The previous state that is being transitioned from.
     */
    public void stopping(State from) {}

    /**
     * Called when the service transitions to the {@code State#TERMINATED TERMINATED} state.
     * The {@code State#TERMINATED TERMINATED} state is a terminal state in the transition
     * diagram. Therefore, if this method is called, no other methods will be called on the {@code
     * Listener}.
     *
     * @param from The previous state that is being transitioned from. Failure can occur in any
     *     state with the exception of {@code State#FAILED FAILED} and {@code
     *     State#TERMINATED TERMINATED}.
     */
    public void terminated(State from) {}

    /**
     * Called when the service transitions to the {@code State#FAILED FAILED} state. The
     * {@code State#FAILED FAILED} state is a terminal state in the transition diagram.
     * Therefore, if this method is called, no other methods will be called on the {@code Listener}.
     *
     * @param from The previous state that is being transitioned from. Failure can occur in any
     *     state with the exception of {@code State#NEW NEW} or {@code State#TERMINATED
     *     TERMINATED}.
     * @param failure The exception that caused the failure.
     */
    public void failed(State from, Throwable failure) {}
  }
}
