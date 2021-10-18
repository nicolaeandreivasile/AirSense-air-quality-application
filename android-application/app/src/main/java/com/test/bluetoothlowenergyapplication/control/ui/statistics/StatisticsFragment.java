package com.test.bluetoothlowenergyapplication.control.ui.statistics;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.test.bluetoothlowenergyapplication.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class StatisticsFragment extends Fragment {
    public final static String ENTRIES = "Entries";

    public final static int DEFAULT_ENTRY_LIMIT = 90;

    private StatisticsViewModel statisticsViewModel;

    private ArrayList<Entry> statisticsEntryList;

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.control_fragment_statistics_layout,
                container, false);

        return view;
    }

    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
        LineChart statisticsChart = (LineChart) view.findViewById(R.id.statisticsLineChart);

        /* Get the StatisticViewModel and retrieve saved values */
        statisticsViewModel =
                new ViewModelProvider(requireActivity()).get(StatisticsViewModel.class);
        if (!retrieveStatistics() || statisticsEntryList.isEmpty())
            return;

        /* Prepare data for visualization */
        LineDataSet statisticsDataSet = initStatisticsDataSet(statisticsEntryList);
        LineData statisticsData = new LineData(statisticsDataSet);
        statisticsChart.setData(statisticsData);
        statisticsChart.getDescription().setEnabled(false);

        /* Set X axis style */
        XAxis xAxis = statisticsChart.getXAxis();
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawLabels(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTypeface(Typeface.DEFAULT_BOLD);
        xAxis.setAxisLineColor(R.color.black);
        xAxis.setAxisMinimum(0);

        /* Disable right Y axis */
        YAxis yAxisRight = statisticsChart.getAxisRight();
        yAxisRight.setEnabled(false);

        /* Set left Y axis style */
        YAxis yAxisLeft = statisticsChart.getAxisLeft();
        yAxisLeft.setDrawAxisLine(true);
        yAxisLeft.setDrawGridLines(false);
        yAxisLeft.setDrawLabels(true);
        yAxisLeft.setTypeface(Typeface.DEFAULT_BOLD);
        yAxisLeft.setAxisLineColor(R.color.black);
        yAxisLeft.setAxisMinimum(getResources().getInteger(R.integer.statistics_y_minimum));
        yAxisLeft.setAxisMaximum(getResources().getInteger(R.integer.statistics_y_maximum));
        yAxisLeft.setLabelCount(getResources().getInteger(R.integer.limit_no), true);
        yAxisLeft.setDrawLimitLinesBehindData(true);
        ArrayList<LimitLine> limitLineList = initStatisticsLimitLines();
        for (LimitLine limitLine : limitLineList)
            yAxisLeft.addLimitLine(limitLine);

        /* Refresh line chart */
        statisticsChart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        saveStatistics();
    }

    /* Initialize the data set */
    private LineDataSet initStatisticsDataSet(ArrayList<Entry> statisticsEntryList) {
        LineDataSet statisticsDataSet = new LineDataSet(statisticsEntryList,
                "Pollution index");

        statisticsDataSet.setDrawValues(false);
        statisticsDataSet.setDrawCircles(false);
        statisticsDataSet.setColor(getResources().getColor(R.color.orange_500, null));
        statisticsDataSet.setLineWidth(statisticsDataSet.getLineWidth() * 1.1f);
        statisticsDataSet.setMode(LineDataSet.Mode.LINEAR);

        return statisticsDataSet;
    }

    /* Define the limit lines for pollution levels accordingly */
    private ArrayList<LimitLine> initStatisticsLimitLines() {
        ArrayList<LimitLine> limitLineList = new ArrayList<LimitLine>();

        LimitLine excellentLimitLine = new LimitLine(getResources()
                .getInteger(R.integer.excellent_limit_begin), "Excellent");
        excellentLimitLine.setLineColor(getResources().getColor(R.color.green, null));
        excellentLimitLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        limitLineList.add(excellentLimitLine);

        LimitLine goodLimitLine = new LimitLine(getResources()
                .getInteger(R.integer.good_limit_begin), "Good");
        goodLimitLine.setLineColor(getResources().getColor(R.color.pale_green, null));
        goodLimitLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        limitLineList.add(goodLimitLine);

        LimitLine lightlyPollutedLimitLine = new LimitLine(getResources()
                .getInteger(R.integer.lightly_limit_begin), "Lightly");
        lightlyPollutedLimitLine.setLineColor(getResources().getColor(R.color.yellow, null));
        lightlyPollutedLimitLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        limitLineList.add(lightlyPollutedLimitLine);

        LimitLine moderatelyPollutedLimitLine = new LimitLine(getResources()
                .getInteger(R.integer.moderately_limit_begin), "Moderately");
        moderatelyPollutedLimitLine.setLineColor(
                getResources().getColor(R.color.orange, null));
        moderatelyPollutedLimitLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        limitLineList.add(moderatelyPollutedLimitLine);

        LimitLine heavilyPollutedLimitLine = new LimitLine(getResources()
                .getInteger(R.integer.heavily_limit_begin), "Heavily");
        heavilyPollutedLimitLine.setLineColor(getResources().getColor(R.color.red, null));
        heavilyPollutedLimitLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        limitLineList.add(heavilyPollutedLimitLine);

        LimitLine severelyPollutedLimitLine = new LimitLine(getResources()
                .getInteger(R.integer.severely_limit_begin), "Severely");
        severelyPollutedLimitLine.setLineColor(
                getResources().getColor(R.color.purple, null));
        severelyPollutedLimitLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        limitLineList.add(severelyPollutedLimitLine);

        LimitLine extremelyPollutedLimitLine = new LimitLine(getResources().
                getInteger(R.integer.extremely_limit_begin), "Extremely");
        extremelyPollutedLimitLine.setLineColor(
                getResources().getColor(R.color.brown, null));
        extremelyPollutedLimitLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        limitLineList.add(extremelyPollutedLimitLine);

        return limitLineList;
    }


    /* Save data using StatisticsViewModel */
    private void saveStatistics() {
        if (statisticsViewModel == null)
            return;

        statisticsViewModel.setPersistentArrayField(ENTRIES, statisticsEntryList);
    }

    /* Retrieve data from the StatisticsViewModel */
    private boolean retrieveStatistics() {
        if (statisticsViewModel == null)
            return false;

        statisticsEntryList = statisticsViewModel.getPersistentArrayField(ENTRIES);
        if (statisticsEntryList == null)
            return false;

        return true;
    }
}
