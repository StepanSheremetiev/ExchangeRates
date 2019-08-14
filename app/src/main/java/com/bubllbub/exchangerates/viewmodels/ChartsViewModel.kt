package com.bubllbub.exchangerates.viewmodels

import android.content.ContentValues
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bubllbub.exchangerates.App
import com.bubllbub.exchangerates.di.DaggerAppComponent
import com.bubllbub.exchangerates.di.modules.AppModule
import com.bubllbub.exchangerates.di.modules.RepositoryModule
import com.bubllbub.exchangerates.models.*
import com.bubllbub.exchangerates.objects.Currency
import com.bubllbub.exchangerates.objects.Rate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import io.reactivex.subscribers.DisposableSubscriber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


class ChartsViewModel : ViewModel() {
    var currencies = MutableLiveData<List<Currency>>()
    var rates = MutableLiveData<List<Rate>>()
    var isLoading = ObservableField(true)
    @Inject
    lateinit var rateRepo: Repo<Rate>
    @Inject
    lateinit var currencyRepo: Repo<Currency>
    private val compositeDisposable = CompositeDisposable()

    init {
        DaggerAppComponent.builder()
            .appModule(AppModule(App.instance))
            .repositoryModule(RepositoryModule())
            .build()
            .inject(this)
    }

    fun refresh(currencyId: Int, currencyAbbreviation: String, startDate: Date, finishDate: Date) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        compositeDisposable.add(

            rateRepo.query()
                .where(CUR_ID, currencyId.toString())
                .where(CUR_ABBREVIATION, currencyAbbreviation)
                .where(START_DATE, dateFormat.format(startDate))
                .where(END_DATE, dateFormat.format(finishDate))
                .findAll()
                .firstOrError()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<List<Rate>>() {
                    override fun onSuccess(t: List<Rate>) {
                        rates.value = t
                        isLoading.set(false)
                        Log.d(ContentValues.TAG, "[onNext] " + t.toString())
                    }

                    override fun onError(t: Throwable) {
                        Log.d(ContentValues.TAG, "[onError] ")
                        t.printStackTrace()
                    }
                })
        )
    }

    fun getActualList() {
        compositeDisposable.add(
            currencyRepo.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSubscriber<List<Currency>>() {
                    override fun onComplete() {
                        Log.d(ContentValues.TAG, "[onCompleted] ")
                    }

                    override fun onError(t: Throwable) {
                        Log.d(ContentValues.TAG, "[onError] ")
                        t.printStackTrace()
                    }

                    override fun onNext(m: List<Currency>) {
                        currencies.value = m
                        isLoading.set(false)
                        Log.d(ContentValues.TAG, "[onNext] " + m.toString())
                    }
                })
        )
    }

    override fun onCleared() {
        super.onCleared()
        if (!compositeDisposable.isDisposed) {
            compositeDisposable.dispose()
        }
    }
}