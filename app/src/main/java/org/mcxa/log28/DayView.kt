package org.mcxa.log28

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.xwray.groupie.ExpandableGroup
import devs.mulham.horizontalcalendar.HorizontalCalendar
import java.util.*
import devs.mulham.horizontalcalendar.utils.HorizontalCalendarListener
import kotlinx.android.synthetic.main.fragment_day_view.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.kotlinandroidextensions.*
import org.mcxa.log28.org.mcxa.log28.groupie.ChildItem
import org.mcxa.log28.org.mcxa.log28.groupie.ExpandableHeaderItem
import org.mcxa.log28.org.mcxa.log28.groupie.NotesItem

/**
 * Handles the day view
 */
class DayView : Fragment() {
    // changes both the data displayed and the date at the top.
    lateinit var navigateToDay: (c: Calendar) -> Unit

    private val categories = getCategories()
    private val symptoms = getSymptoms()

    private val groupAdapter = GroupAdapter<ViewHolder>()

    // we store our categories and symptom groups here so we can update them
    private val categoryGroup = mutableListOf<ExpandableGroup>()
    private val symptomList = mutableListOf<MutableList<ChildItem>>()
    // reference to the notes and sleep amount
    private val notesAndSleep = Section()
    private lateinit var notesItem: NotesItem

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_day_view, container, false)
        setupHorizontalCalendar(rootView)
        return rootView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        categories.addChangeListener {
            _, changeSet ->
            if (changeSet != null)
                groupAdapter.notifyDataSetChanged()
            Log.d("DAYVIEW", "categories updated $changeSet")
        }

        setupRecyclerView()
    }

    override fun onDestroy() {
        super.onDestroy()
        categories.removeAllChangeListeners()
    }

    // setup the recycler view
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        day_view_recycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = groupAdapter
        }
    }

    private fun setupHorizontalCalendar(rootView: View) {
        // setup the top calendar
        val startDate = Calendar.getInstance()
        startDate.add(Calendar.MONTH, -1)
        val endDate = Calendar.getInstance()
        //endDate.add(Calendar.DAY_OF_MONTH, 1)

        val currentdate = Calendar.getInstance()

        val horizontalCalendar = HorizontalCalendar.Builder(rootView, R.id.topCalendar)
                .defaultSelectedDate(currentdate)
                .range(startDate, endDate)
                .datesNumberOnScreen(5).build()

        navigateToDay = {
            c -> // set the range to be one month before
            //TODO clean this mess up
            if (c.before(startDate)) {
                startDate.set(Calendar.YEAR, c.get(Calendar.YEAR))
                startDate.set(Calendar.MONTH, c.get(Calendar.MONTH))
                startDate.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH))
                startDate.add(Calendar.MONTH, -1)
                Log.d("DAYVIEW", "navtoday seting startdate to ${startDate.formatDate()}")
                horizontalCalendar.setRange(startDate, endDate)
                horizontalCalendar.refresh()
            }
            horizontalCalendar.selectDate(c, true)
        }

        horizontalCalendar.calendarListener = object : HorizontalCalendarListener() {
            override fun onDateSelected(date: Calendar, position: Int) {
                Log.d("DAYVIEW", "horizontal calendar date set to ${date.formatDate()}")
                loadDayData(date)
                // this little bit of code extends the range of the dates
                val cDate = date.clone() as Calendar
                cDate.add(Calendar.DAY_OF_YEAR, -5)
                if (startDate.after(cDate)) {
                    Log.d("DAYVIEW", "setting range")
                    startDate.add(Calendar.MONTH, -1)

                    horizontalCalendar.setRange(startDate, endDate)
                    horizontalCalendar.refresh()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        val daydata = getDataByDate(Calendar.getInstance())

        // add each category as a header
        // add each symptom under a category, set the state based on what's in the DayData object
        categories.forEach { category ->
            ExpandableGroup(ExpandableHeaderItem(category.name)).apply {
                val symptomsInCategory = mutableListOf<ChildItem>()
                symptoms.filter { s -> s.category?.name == category.name }.forEach { symptom ->
                    val childItem = ChildItem(symptom,symptom in daydata.symptoms,
                            // here we pass an update function
                            { daydata.toggleSymptom(context, symptom) })

                    symptomsInCategory.add(childItem)
                }
                symptomList.add(symptomsInCategory)
                this.addAll(symptomsInCategory)
                categoryGroup.add(this)
            }
        }

        notesItem = NotesItem(daydata)
        notesAndSleep.add(notesItem)

        groupAdapter.addAll(categoryGroup)
        groupAdapter.add(notesAndSleep)
    }

    fun loadDayData(day: Calendar) {
        Log.d("DAYVIEW", "Loading data for ${day.formatDate()}")

        val daydata = getDataByDate(day)

        symptomList.forEach {
            it.forEach {
                it.onClick = { daydata.toggleSymptom(context, it.symptom) }
                it.state = it.symptom in daydata.symptoms
            }
        }
        //TODO can we avoid redrawing everything
        categoryGroup.forEach {
            it.notifyChanged()
        }

        notesItem.daydata = daydata
        notesAndSleep.notifyChanged()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment DayView.
         */
        fun newInstance(): DayView {
            val fragment = DayView()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}