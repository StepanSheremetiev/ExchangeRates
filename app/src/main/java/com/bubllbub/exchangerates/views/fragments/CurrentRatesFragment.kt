package com.bubllbub.exchangerates.views.fragments

import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.bubllbub.exchangerates.R
import com.bubllbub.exchangerates.adapters.CurrencyRecyclerAdapter
import com.bubllbub.exchangerates.databinding.ErFragmentCurrentRatesBinding
import com.bubllbub.exchangerates.dialogs.AddCurrencyDialog
import com.bubllbub.exchangerates.dialogs.TAG_FAVORITES
import com.bubllbub.exchangerates.objects.Currency
import com.bubllbub.exchangerates.ui.recyclerview.SwipeDeleteHelper
import com.bubllbub.exchangerates.ui.widgets.SmartDividerItemDecoration
import com.bubllbub.exchangerates.viewmodels.CurrentRatesViewModel
import kotlinx.android.synthetic.main.er_fragment_current_rates.view.*
import javax.inject.Inject

class CurrentRatesFragment : BackDropFragment() {
    private lateinit var binding: ErFragmentCurrentRatesBinding

    lateinit var currentViewModel: CurrentRatesViewModel

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var adapter: CurrencyRecyclerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        binding =
            DataBindingUtil.inflate(inflater, R.layout.er_fragment_current_rates, container, false)
        currentViewModel =
            ViewModelProviders.of(this, viewModelFactory)[CurrentRatesViewModel::class.java]
        binding.lifecycleOwner = viewLifecycleOwner
        binding.currentRatesViewModel = currentViewModel
        binding.executePendingBindings()

        binding.rvCurrentRates.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCurrentRates.adapter = adapter
        binding.rvCurrentRates.addItemDecoration(
            SmartDividerItemDecoration(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.er_recycler_devider
                )
            )
        )

        val itemTouchHelper = ItemTouchHelper(
            SwipeDeleteHelper(
                adapter,
                requireContext(),
                binding.scrollViewFragmentFirst,
                object : SwipeDeleteHelper.SwipeDeleteCallback {
                    override fun onDeleteFromSwipe(currency: Currency) {
                        binding.currentRatesViewModel?.deleteFavCurrency(currency)
                    }

                    override fun onRestoreFromSwipe(currency: Currency) {
                        binding.currentRatesViewModel?.insertFavCurrency(currency)
                    }
                }
            )
        )
        itemTouchHelper.attachToRecyclerView(binding.rvCurrentRates)

        binding.additionalCurrencyBtn.setOnClickListener {
            AddCurrencyDialog().show(childFragmentManager, TAG_FAVORITES)
        }

        currentViewModel.currencies.observe(this,
            Observer<List<Currency>> {
                it?.let { adapter.replaceData(it) }
            }
        )

        val view = binding.root
        setBackDrop(view.app_bar, view.scroll_view_fragment_first)
        return view
    }

    override fun onStart() {
        super.onStart()
        binding.currentRatesViewModel?.getFavorites()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.current_rates_toolbar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add_item -> {
                AddCurrencyDialog().show(childFragmentManager, TAG_FAVORITES)
                true
            }
            else -> false
        }
    }
}