/*
 * Copyright (C) 2019 Buglife, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.buglife.crashlife.sdk;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public final class DebugMenuActivity extends Activity {

    private DebugMenuListAdapter mListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_menu);

        ListView mListView = findViewById(R.id.debug_menu_list);
        ArrayList<RowType> rowTypes = new ArrayList<>();
        rowTypes.add(RowType.ENDPOINT);
        rowTypes.add(RowType.CACHED_CRASH_COUNT);
        rowTypes.add(RowType.CLEAR_CACHE);
        rowTypes.add(RowType.FORCE_CRASH);
        rowTypes.add(RowType.LOG_ERROR);
        mListAdapter = new DebugMenuListAdapter(this, rowTypes);
        mListView.setAdapter(mListAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                RowType row = mListAdapter.getItem(i);

                if (row == RowType.CLEAR_CACHE) {
                    clearCache();
                } else if (row == RowType.FORCE_CRASH) {
                    forceCrash();
                } else if (row == RowType.LOG_ERROR) {
                    logError();
                }
            }
        });
    }

    private void clearCache() {
        Crashlife.getClient().deleteAllCachedEvents();
        mListAdapter.notifyDataSetChanged();
    }

    private void forceCrash() {
        throw new RuntimeException("AwesomeException");
    }

    private void logError() {
        Crashlife.logError("AnAwesomeError");
        mListAdapter.notifyDataSetChanged();
    }

    private class DebugMenuListAdapter extends ArrayAdapter<RowType> {
        private final Context mContext;
        private final ArrayList<RowType> mData;

        DebugMenuListAdapter(Context context, ArrayList<RowType> data) {
            super(context, 0, data);
            this.mContext = context;
            this.mData = data;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View row = convertView;
            RowType rowType = mData.get(position);

            if (row == null) {
                LayoutInflater inflater = ((Activity)mContext).getLayoutInflater();
                int layoutResourceId = R.layout.debug_menu_row;
                row = inflater.inflate(layoutResourceId, parent, false);
            }

            TextView titleView = row.findViewById(R.id.debug_menu_row_title_view);
            TextView detailView = row.findViewById(R.id.debug_menu_row_detail_view);
            String titleString = "";
            String detailString = "";

            switch (rowType) {
                case ENDPOINT:
                    titleString = "Endpoint";
                    detailString = AsyncHttpTask.BASE_URL;
                    break;
                case CACHED_CRASH_COUNT:
                    int count = Crashlife.getClient().getCachedEventCount();
                    titleString = "Count";
                    detailString = Integer.toString(count);
                    break;
                case CLEAR_CACHE:
                    titleString = "Clear cache";
                    detailString = "Tap to clear";
                    break;
                case FORCE_CRASH:
                    titleString = "Force crash";
                    detailString = "RuntimeException(\"AwesomeException\")";
                    break;
                case LOG_ERROR:
                    titleString = "Log Error";
                    detailString = "AnAwesomeError";
                    break;
            }

            titleView.setText(titleString);
            detailView.setText(detailString);

            return row;
        }
    }

    private enum RowType {
        ENDPOINT, CACHED_CRASH_COUNT, CLEAR_CACHE, FORCE_CRASH, LOG_ERROR
    }
}
