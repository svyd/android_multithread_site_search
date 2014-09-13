package com.blogspot.vsvydenko.multithread_site_searcher.ui;

import com.blogspot.vsvydenko.multithread_site_searcher.R;
import com.blogspot.vsvydenko.multithread_site_searcher.entity.UrlItem;
import com.blogspot.vsvydenko.multithread_site_searcher.utils.HttpUtils;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by vsvydenko on 11.09.14.
 */
public class SearchFragment extends Fragment {

    public static final int PROGRESS_MAX = 100;
    public static final String REGEX_PATTERN
            = "(http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";

    private View returnView;

    private int foundUrlCounter = 0;
    private int scannedUrlCounter = 0;
    private int foundTextCounter = 0;
    private int currentLevel = 1;

    private String mBaseUrl;
    private String mText;
    private int mThreadsNumber = 1;
    private int mMaxValueScannedUrls;
    private ConcurrentHashMap<Integer, LinkedList<String>> urlsHashMap
            = new ConcurrentHashMap<Integer, LinkedList<String>>();

    private EditText mBaseUrlEditText;
    private EditText mThreadNumberText;
    private EditText mSearchTextEditText;
    private EditText mMaxValOfScanUrlEditText;

    private Button mStartButton;
    private Button mStopButton;
    private ProgressBar mProgressBar;

    private TextView mFoundTextView;

    private ListView mStatusListView;

    private UrlAdapter mUrlAdapter;
    private CopyOnWriteArrayList mUrlsList;
    private LinkedList<Thread> mThreadLinkedList = new LinkedList<Thread>();
    private boolean isSearchStopped = false;

    public SearchFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        returnView = inflater.inflate(R.layout.fragment_search, container, false);

        return returnView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initializeContent();
    }

    private void initializeContent() {

        mBaseUrlEditText = (EditText) returnView.findViewById(R.id.editBaseUrl);
        mThreadNumberText = (EditText) returnView.findViewById(R.id.editThreadsNumber);
        mSearchTextEditText = (EditText) returnView.findViewById(R.id.editSearchText);
        mMaxValOfScanUrlEditText = (EditText) returnView.findViewById(R.id.editScannedUrlsNumber);

        mStartButton = (Button) returnView.findViewById(R.id.btnStart);
        mStopButton = (Button) returnView.findViewById(R.id.btnStop);

        mProgressBar = (ProgressBar) returnView.findViewById(R.id.progress);
        mProgressBar.setIndeterminate(false);
        mProgressBar.setProgress(0);
        mProgressBar.setMax(PROGRESS_MAX);

        mFoundTextView = (TextView) returnView.findViewById(R.id.txtFound);

        mStatusListView = (ListView) returnView.findViewById(R.id.lstUrlStatus);

        View.OnClickListener click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.btnStart:
                        startSearch();
                        break;
                    case R.id.btnStop:
                        stopSearch();
                        break;
                }
            }
        };

        mStartButton.setOnClickListener(click);
        mStopButton.setOnClickListener(click);

    }

    private void startSearch() {
        resetData();
        if (TextUtils.isEmpty(mBaseUrlEditText.getText()) ||
                TextUtils.isEmpty(mThreadNumberText.getText()) ||
                TextUtils.isEmpty(mSearchTextEditText.getText()) ||
                TextUtils.isEmpty(mMaxValOfScanUrlEditText.getText())) {
            Toast.makeText(getActivity(), getString(R.string.check_input_data), Toast.LENGTH_LONG)
                    .show();
            return;
        } else {

            setEnabledParameterUIState(false);
            mBaseUrl = mBaseUrlEditText.getText().toString().trim();
            mThreadsNumber = Integer.parseInt(mThreadNumberText.getText().toString());
            mText = mSearchTextEditText.getText().toString().trim();
            mMaxValueScannedUrls = Integer.parseInt(mMaxValOfScanUrlEditText.getText().toString());

            urlsHashMap.put(currentLevel, new LinkedList<String>());
            urlsHashMap.get(currentLevel).add(mBaseUrl);

            doSearch();
        }
    }

    private void stopSearch() {
        setEnabledParameterUIState(true);
        isSearchStopped = true;
        for (Thread thread : mThreadLinkedList) {
            thread.interrupt();
        }
    }

    private void doSearch() {
        final ValueContainer linksForCurrLevel = new ValueContainer(
                urlsHashMap.get(currentLevel).size());
        final AtomicInteger threadCounter = new AtomicInteger(0);
        search(linksForCurrLevel, threadCounter);
    }

    private void search(final ValueContainer linksForCurrLevel, final AtomicInteger threadCounter) {
        if (isSearchStopped) {
            return;
        }
        if (linksForCurrLevel.getVal() > 0 && threadCounter.intValue() < mThreadsNumber) {
            threadCounter.incrementAndGet();
            final int activeUrlId = linksForCurrLevel.decrementAndGet();
            scannedUrlCounter++;
            Thread thread = new Thread(new Runnable() {
                String response = null;

                @Override
                public void run() {
                    if (isSearchStopped) {
                        return;
                    }
                    try {
                        response = HttpUtils.doRequest(
                                urlsHashMap.get(currentLevel).get(activeUrlId));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (!TextUtils.isEmpty(response) && !response.startsWith("Error occured!")) {
                        if (Thread.interrupted()) {
                            return;
                        }
                        searchUrls(response);
                        final int found = searchText(response);
                        if (mUrlsList == null) {
                            mUrlsList = new CopyOnWriteArrayList<UrlItem>();
                        }

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mUrlsList.add(new UrlItem(urlsHashMap.get(currentLevel)
                                        .get(activeUrlId),
                                        getString(R.string.success), found));
                                updateUI();
                            }
                        });
                    } else {
                        if (Thread.interrupted()) {
                            return;
                        }
                        if (mUrlsList == null) {
                            mUrlsList = new CopyOnWriteArrayList<UrlItem>();
                        }

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mUrlsList.add(new UrlItem(urlsHashMap.get(currentLevel)
                                        .get(linksForCurrLevel.getVal()),
                                        String.format(getString(R.string.error),
                                                response), 0));
                                updateUI();
                            }
                        });

                    }

                    if (isSearchCompleted()) {
                        return;
                    }
                    boolean levelIsCompleted = threadCounter.decrementAndGet() == 0;
                    if (levelIsCompleted && linksForCurrLevel.isZero()) {
                        currentLevel++;
                        doSearch();
                    } else {
                        for (int i = threadCounter.intValue();
                                (i < mThreadsNumber && !linksForCurrLevel.isZero()); i++) {
                            search(linksForCurrLevel, threadCounter);
                        }
                    }

                }
            });
            mThreadLinkedList.add(thread);
            thread.start();
        }
    }

    private int searchUrls(String response) {
        int value = 0;
        int level = currentLevel + 1;
        Pattern p = Pattern.compile(REGEX_PATTERN);
        Matcher matcher = p.matcher(response);
        while (matcher.find()) {
            value++;
            foundUrlCounter++;
            if (urlsHashMap.get(level) == null) {
                urlsHashMap.put(level, new LinkedList<String>());
            }
            urlsHashMap.get(level).add(matcher.group());
        }
        return value;
    }

    private int searchText(String response) {
        int value = 0;
        Pattern p = Pattern.compile(mText);
        Matcher matcher = p.matcher(response);
        while (matcher.find()) {
            value++;
            foundTextCounter++;
        }
        return value;
    }

    private void setEnabledParameterUIState(boolean value) {
        mBaseUrlEditText.setEnabled(value);
        mThreadNumberText.setEnabled(value);
        mSearchTextEditText.setEnabled(value);
        mMaxValOfScanUrlEditText.setEnabled(value);
        if (value) {
            mStartButton.setEnabled(true);
            mStopButton.setEnabled(false);
        } else {
            mStartButton.setEnabled(false);
            mStopButton.setEnabled(true);
        }
    }

    private void updateUI() {
        mProgressBar.setProgress(scannedUrlCounter * PROGRESS_MAX / mMaxValueScannedUrls);
        mFoundTextView.setText(String.format(getString(R.string.found), foundTextCounter));

        if (mUrlAdapter == null) {
            mUrlAdapter = new UrlAdapter(getActivity(), mUrlsList);
        }

        if (mStatusListView.getAdapter() == null) {
            mStatusListView.setAdapter(mUrlAdapter);
        } else {
            mUrlAdapter.notifyDataSetChanged();
        }
    }

    private boolean isSearchCompleted() {
        if (scannedUrlCounter >= mMaxValueScannedUrls && !isSearchStopped) {
            isSearchStopped = true;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(),
                            getString(R.string.completed), Toast.LENGTH_LONG)
                            .show();
                    stopSearch();
                }
            });
            return true;
        } else {
            return false;
        }

    }

    private void resetData() {
        scannedUrlCounter = 0;
        mMaxValueScannedUrls = 1;
        mThreadsNumber = 1;
        foundTextCounter = 0;
        foundUrlCounter = 0;
        mThreadLinkedList.clear();
        isSearchStopped = false;
        mText = "";
        mProgressBar.setProgress(0);
        mFoundTextView.setText(String.format(getString(R.string.found), foundTextCounter));
        mUrlAdapter = null;
        if (mUrlsList != null) {
            mUrlsList.clear();
        }
        if (urlsHashMap != null) {
            urlsHashMap.clear();
        }
        if (mStatusListView != null) {
            mStatusListView.setAdapter(null);
        }
    }

    public class ValueContainer {

        private int val;

        public ValueContainer() {
        }

        public ValueContainer(int v) {
            this.val = v;
        }

        public int getVal() {
            return val;
        }

        public void setVal(int val) {
            this.val = val;
        }

        public int decrementAndGet() {
            return --this.val;
        }

        public boolean isZero() {
            return val == 0;
        }
    }


}
