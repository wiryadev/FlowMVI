# FlowMVI 2.0

[![CI](https://github.com/respawn-app/FlowMVI/actions/workflows/ci.yml/badge.svg)](https://github.com/respawn-app/FlowMVI/actions/workflows/ci.yml)
![Docs](https://img.shields.io/website?down_color=red&down_message=Offline&label=Docs&up_color=green&up_message=Online&url=https%3A%2F%2Fopensource.respawn.pro%2FFlowMVI%2F%23%2F)
[![Javadoc](https://javadoc.io/badge2/pro.respawn.flowmvi/core/javadoc.svg)](https://javadoc.io/doc/pro.respawn.flowmvi/core)
![License](https://img.shields.io/github/license/respawn-app/flowMVI)
![GitHub last commit](https://img.shields.io/github/last-commit/respawn-app/FlowMVI)
![Issues](https://img.shields.io/github/issues/respawn-app/FlowMVI)
![GitHub top language](https://img.shields.io/github/languages/top/respawn-app/flowMVI)
[![CodeFactor](https://www.codefactor.io/repository/github/respawn-app/flowMVI/badge)](https://www.codefactor.io/repository/github/respawn-app/flowMVI)
[![AndroidWeekly #556](https://androidweekly.net/issues/issue-556/badge)](https://androidweekly.net/issues/issue-556/)

### This is a readme for FlowMVI 2.0, which is in alpha right now. Please go to the [V1 Docs](https://opensource.respawn.pro/FlowMVI/v1/#/) if you need to see the older version

FlowMVI is a Kotlin Multiplatform MVI library based on coroutines that has a few main goals:

1. Being simple to understand and use while staying powerful and flexible.
2. Featuring a clean and rich DSL.
3. Being thread-safe but asynchronous by design.

## Quickstart:

* Documentation: [https://opensource.respawn.pro/FlowMVI/](https://opensource.respawn.pro/FlowMVI)
* KDocs: [https://opensource.respawn.pro/FlowMVI/javadocs/](https://opensource.respawn.pro/FlowMVI/javadocs/)
* Latest
  version: ![Maven Central](https://img.shields.io/maven-central/v/pro.respawn.flowmvi/core?label=Maven%20Central)

```toml
[versions]
flowmvi = "< Badge above 👆🏻 >"

[dependencies]
flowmvi-core = { module = "pro.respawn.flowmvi:core", version.ref = "flowmvi" } # multiplatform
flowmvi-android = { module = "pro.respawn.flowmvi:android", version.ref = "flowmvi" } # common android
flowmvi-view = { module = "pro.respawn.flowmvi:android-view", version.ref = "flowmvi" } # view-based android
flowmvi-compose = { module = "pro.respawn.flowmvi:android-compose", version.ref = "flowmvi" }  # compose
```

Supported platforms:

* JVM: [ Android ],
* Linux [ X64, Mingw64 ],
* iOS: [ X64, Arm64, macOS ],
* js: [ nodejs, browser ]

### Core:

```kotlin
sealed interface CounterState : MVIState {
    data object Loading : CounterState
    data class Error(e: Exception) : CounterState
    data class DisplayingCounter(
        val timer: Int,
        val counter: Int,
    ) : CounterState
}

sealed interface CounterIntent : MVIIntent {
    data object ClickedCounter : CounterIntent
}

sealed interface CounterAction : MVIAction {
    data class ShowMessage(val message: String) : CounterAction
}

class CounterContainer(
    private val repo: CounterRepository,
) {
    val store by store<CounterState, CounterIntent, CounterAction>(Loading) { // set initial state
        name = "CounterStore"
        parallelIntents = true
        actionShareBehavior = ActionShareBehavior.Restrict() // disable, share, distribute or consume side effects
        intentCapacity = 64

        install(consoleLoggingPlugin()) // log to console, logcat, or NSLog
        install(analyticsPlugin(name)) // create custom plugins 
        install(timeTravelPlugin()) // unit test stores and track changes

        saveState {  // persist and restore state
            get = { repo.restoreStateFromFile() }
            set = { it: CounterState -> repo.saveToFile(it) }
        }
        init { // run actions when store is launched
            repo.startTimer()
        }
        whileSubscribed { // run a job while any subscribers are present
            repo.timer.onEach { timer: Int ->
                updateState<DisplayingCounter> { // update state safely between threads and filter by type
                    copy(timer = timer)
                }
            }.consume()
        }
        recover { e: Exception -> // recover from errors both in jobs and plugins
            send(CounterAction.ShowMessage(e.message)) // send side-effects
            updateState {
                Error(e)
            }
            null
        }
        reduce { intent: CounterIntent -> // reduce intents
            when (intent) {
                is ClickedCounter -> updateState<DisplayingCounter> {
                    copy(counter = counter + 1)
                }
            }
        }
        install { // build and install custom plugins on the fly
            onStop { // hook into various store events
                repo.stopTimer()
            }
            onState { old, new -> // veto changes, modify states, launch jobs, do literally anything
                new.withType<DisplayingCounter, _> {
                    if (counter >= 100) {
                        launch { repo.resetTimer() }
                        copy(counter = 0, timer = 0)
                    } else new
                }
            }
        }
    }
}

// somewhere in the ui layer...

```

Subscribe one-liner:

```kotlin
store.subscribe(
    scope = consumerCoroutineScope,
    consume = { action -> /* process side effects */ },
    render = { state -> /* render states */ },
)
```

Custom plugins:

```kotlin
// create plugins for any store 
fun analyticsPlugin(name: String) = genericPlugin {
        val analytics = Analytics.getInstance()
        onStart {
            analytics.log("Screen $name opened")
        }
        onIntent {
            analytics.log(intent.asAnalyticsEvent())
        }
        // 5+ more hooks
    }

// or for a specific one
val counterPlugin = plugin<CounterState, CounterIntent, CounterAction> {
    /*...*/
}

```

### Android (Compose):

```kotlin
val module = module {
    factoryOf(::CounterContainer)
    // No more subclassing. Use StoreViewModel for everything and inject containers or stores directly.
    viewModel(qualifier<CounterContainer>()) { StoreViewModel(get<CounterContainer>().store) }
}

@Composable
fun CounterScreen() = MVIComposable(
    provider = getViewModel<StoreViewModel>(qualifier<CounterContainer>()),
) { state ->

    consume { action ->
        when (action) {
            is ShowMessage -> {
                /* ... */
            }
        }
    }

    when (state) {
        is DisplayingCounter -> {
            Button(onClick = { ClickedCounter.send() }) {
                Text("Counter: ${state.counter}")
            }
        }
    }
}
```

### Android (View):

```kotlin

// ViewModel and Model classes have not changed

class ScreenFragment : Fragment(), MVIView<CounterState, CounterIntent, CounterAction> {

    override val provider by viewModel<StoreViewModel>(qualifier<CounterContainer>())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribe() // One-liner for store subscription. Lifecycle-aware and efficient.
    }

    override fun render(state: CounterState) {
        // update your views
    }

    override fun consume(action: CounterAction) {
        // handle actions
    }
}
```

Ready to try? Start with reading the [Quickstart Guide](https://opensource.respawn.pro/FlowMVI/quickstart).

## License

```
   Copyright 2022-2023 Respawn Team and contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

```
