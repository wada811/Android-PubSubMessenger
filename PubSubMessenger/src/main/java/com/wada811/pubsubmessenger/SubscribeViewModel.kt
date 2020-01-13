package com.wada811.pubsubmessenger

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.Job

internal class SubscribeViewModel<T : PubSubMessage>(private val clazz: Class<T>, private val onReceive: (T) -> Unit) :
    ViewModel(), LifecycleOwner {
    companion object {
        fun <T : PubSubMessage> subscribeMessage(
            viewModelStoreOwner: ViewModelStoreOwner,
            lifecycle: Lifecycle,
            clazz: Class<T>,
            onReceive: (T) -> Unit
        ): Job {
            val viewModel = ViewModelProvider(viewModelStoreOwner, object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel?> create(modelClass: Class<T>): T = SubscribeViewModel(clazz, onReceive) as T
            }).get("${clazz.name}-$onReceive", SubscribeViewModel::class.java)
            viewModel.observeLifecycle(lifecycle)
            return viewModel.job
        }
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle = lifecycleRegistry
    private val job get() = PubSubMessenger.subscribeMessage<T>(lifecycle, clazz, onReceive)
    private fun observeLifecycle(lifecycle: Lifecycle) {
        when (lifecycle.currentState) {
            Lifecycle.State.INITIALIZED -> Unit
            Lifecycle.State.DESTROYED -> {
                lifecycleRegistry.currentState = Lifecycle.State.CREATED
                lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            }
            else -> {
                lifecycleRegistry.currentState = lifecycle.currentState
            }
        }
        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Event) {
                if (event != ON_DESTROY) {
                    lifecycleRegistry.handleLifecycleEvent(event)
                } else {
                    lifecycle.removeObserver(this)
                }
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
        lifecycleRegistry.handleLifecycleEvent(ON_DESTROY)
    }
}