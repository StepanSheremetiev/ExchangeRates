package com.bubllbub.exchangerates.models.retrofit.apiDatas

import com.bubllbub.exchangerates.enums.CurrencyRes
import com.bubllbub.exchangerates.models.*
import com.bubllbub.exchangerates.models.retrofit.JSONNbrbAPI
import com.bubllbub.exchangerates.objects.Currency
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import java.util.*
import javax.inject.Inject

class CurrencyApiData @Inject constructor(private val jSONApi: JSONNbrbAPI) : DataSource<Currency> {
    private val startingCurrenciesAbbreviations = listOf("USD", "EUR", "RUB")

    override fun getAll(): Flowable<List<Currency>> {
        val requests = arrayListOf<Flowable<Currency>>()

        return jSONApi.getCurrencies()
            .map {
                it.filter { curr ->
                    curr.curDateStart <= Date() && curr.curDateEnd >= Date()
                }
            }
            .flatMap { list ->
                list.forEach { curr ->
                    val ratesAPI = jSONApi.getRatesWithID(curr.curId)
                        .flatMap { rate ->
                            curr.date = rate.date
                            curr.curOfficialRate = rate.curOfficialRate
                            curr.symbol = CurrencyRes.valueOf(curr.curAbbreviation).symbolRes
                            Observable.just(curr)
                        }
                    requests.add(ratesAPI.toFlowable(BackpressureStrategy.LATEST))
                }
                Flowable.zip(requests) {
                    it.map { obj -> obj as Currency }
                }
            }
    }

    override fun getAll(query: DataSource.Query<Currency>): Flowable<List<Currency>> {
        return when {
            (query.has(CUR_CONVERTER) || query.has(CUR_FAVORITE)) -> {
                jSONApi.getCurrencies()
                    .map {
                        it.filter { curr ->
                            curr.curDateStart <= Date() && curr.curDateEnd >= Date() && startingCurrenciesAbbreviations.contains(
                                curr.curAbbreviation
                            )
                        }
                    }
                    .flatMap { list ->
                        val requests = arrayListOf<Flowable<Currency>>()

                        list
                            .sortedBy { CurrencyRes.valueOf(it.curAbbreviation).ordinal }
                            .forEachIndexed { ind, curr ->
                                val ratesAPI = jSONApi.getRatesWithID(curr.curId)
                                    .flatMap { rate ->
                                        curr.date = rate.date
                                        curr.curOfficialRate = rate.curOfficialRate
                                        curr.symbol =
                                            CurrencyRes.valueOf(curr.curAbbreviation).symbolRes
                                        curr.isConverter = true
                                        curr.isFavorite = true
                                        curr.favoritePos = ind + 1
                                        curr.converterPos = ind + 1
                                        Observable.just(curr)
                                    }
                                requests.add(ratesAPI.toFlowable(BackpressureStrategy.LATEST))
                            }

                        Flowable.zip(requests) { listWithRates ->
                            listWithRates.map { obj -> obj as Currency }
                        }
                    }
            }
            else -> throw IllegalArgumentException("Unsupported query $query for Currency")
        }
    }

    override fun get(query: DataSource.Query<Currency>): Observable<Currency> {
        return when {
            (query.has(CUR_ABBREVIATION) && query.has(CUR_DATE)) -> {
                val curAbbreviation = query.get(CUR_ABBREVIATION)
                val curDate = query.get(CUR_DATE)

                jSONApi.getRatesOnDateWithName(curAbbreviation!!, curDate!!)
                    .flatMap { rateCurr ->
                        jSONApi.getCurrencyWithID(rateCurr.curId)
                            .flatMap {
                                it.date = rateCurr.date
                                it.curOfficialRate = rateCurr.curOfficialRate
                                it.symbol = CurrencyRes.valueOf(it.curAbbreviation).symbolRes
                                Observable.just(it)
                            }
                    }
            }
            else -> throw IllegalArgumentException("Unsupported query $query for Currency")
        }
    }

    override fun delete(item: Currency): Completable {
        TODO("not implemented")
    }

    override fun save(item: Currency): Completable {
        TODO("not implemented")
    }

    override fun saveAll(list: List<Currency>): Completable {
        TODO("not implemented")
    }
}
