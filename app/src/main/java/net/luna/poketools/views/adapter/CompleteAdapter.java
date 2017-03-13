package net.luna.poketools.views.adapter;

import android.content.Context;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.omkarmoghe.pokemap.R;

import net.luna.common.debug.LunaLog;
import net.luna.common.util.ListUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CompleteAdapter extends BaseAdapter implements Filterable {
    private class CompleteFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            try {
                if (prefix == null) {
                    return new FilterResults();
                }

                resultList.clear();
                for (CompleteItem item : originalList) {
                    LunaLog.d("ITEM: " + item.getTitle());
                    String title = item.getTitle().toLowerCase();
                    prefix = ((String) prefix).toLowerCase();
                    if (title.contains(prefix)) {
                        item.setIndex(title.indexOf(prefix.toString()));
                        resultList.add(item);
                        if (resultList.size() > 20) {
                            break;
                        }
                    }
                }

                Collections.sort(resultList, new Comparator<CompleteItem>() {
                    @Override
                    public int compare(CompleteItem first, CompleteItem second) {
                        if (first.getIndex() < second.getIndex()) {
                            return -1;
                        } else if (first.getIndex() > second.getIndex()) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                });

                FilterResults results = new FilterResults();
                results.values = resultList;
                results.count = resultList.size();
                return results;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new FilterResults();
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            notifyDataSetChanged();
        }
    }

    @Override
    public void notifyDataSetChanged() {
        if (Looper.myLooper() == Looper.getMainLooper())
            super.notifyDataSetChanged();
    }

    private class CompleteItem {
        private String title;

        protected String getTitle() {
            return title;
        }


        private int index = Integer.MAX_VALUE;

        protected int getIndex() {
            return index;
        }

        protected void setIndex(int index) {
            this.index = index;
        }

        protected CompleteItem(String title) {
            this.title = title;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof CompleteItem)) {
                return false;
            }

            CompleteItem item = (CompleteItem) object;
            return item.getTitle().equals(title);
        }

        @Override
        public int hashCode() {
            if (title == null) {
                return 0;
            }

            return title.hashCode();
        }
    }

    private static class Holder {
        protected TextView nameView;
    }

    private Context context;
    private List<CompleteItem> originalList;
    private List<CompleteItem> resultList;
    private CompleteFilter filter = new CompleteFilter();

    public CompleteAdapter(Context context, List<String> recordList) {
        this.context = context;
        this.originalList = new ArrayList<>();
        this.resultList = new ArrayList<>();
        switchRecord(recordList);
    }

    public void switchRecord(List<String> recordList) {
        for (String record : recordList) {
            if (record != null) {
                CompleteItem completeItem = new CompleteItem(record);
                originalList.add(completeItem);
            }
        }
    }


    @Override
    public int getCount() {
        return ListUtils.getSize(resultList);
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    public Object getItem(int position) {
        try {
            if (ListUtils.getSize(resultList) > position) {
                return resultList.get(position);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        Holder holder;

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.complete_item, null, false);
            holder = new Holder();
            holder.nameView = (TextView) view.findViewById(R.id.complete_item_name);
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }
        try {
            if (ListUtils.getSize(resultList) > position) {
                CompleteItem item = resultList.get(position);
                holder.nameView.setText(item.getTitle());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return view;
    }
}
