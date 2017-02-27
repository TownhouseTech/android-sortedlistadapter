package com.github.wrdlbrnft.sortedlistadapter;

import android.content.Context;
import android.support.v4.util.LongSparseArray;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Created with Android Studio
 * User: Xaver
 * Date: 13/08/16
 */
public abstract class SortedListAdapter<T extends SortedListAdapter.ViewModel> extends RecyclerView.Adapter<SortedListAdapter.ViewHolder<? extends T>> {

    public interface Editor<T extends ViewModel> {
        Editor<T> add(T item);
        Editor<T> add(List<T> items);
        Editor<T> remove(T item);
        Editor<T> remove(List<T> items);
        Editor<T> replaceAll(List<T> items);
        Editor<T> replace(T item);
        Editor<T> removeAll();
        void commit();

        List<T> filterContains(List<T> items);
        List<T> filterFirst(List<T> items);

        List<T> filterContainsAndNew(List<T> incomingList);

        Editor<T> removeByIndex(int position, int count);
    }

    public interface Filter<T> {
        boolean test(T item);
    }

    private final LayoutInflater mInflater;
    private final SortedList<T> mSortedList;
    public List<T> getList(){
        List<T> list = new ArrayList<>();
        for(int i = 0; i < mSortedList.size(); i ++){
            list.add(mSortedList.get(i));
        }
        return list;
    }
    private final Comparator<T> mComparator;

    public SortedListAdapter(Context context, Class<T> itemClass, Comparator<T> comparator) {
        mInflater = LayoutInflater.from(context);
        mComparator = comparator;

        mSortedList = new SortedList<>(itemClass, new SortedList.Callback<T>() {
            @Override
            public int compare(T a, T b) {
                return mComparator.compare(a, b);
            }

            @Override
            public void onInserted(int position, int count) {
                notifyItemRangeInserted(position, count);
            }

            @Override
            public void onRemoved(int position, int count) {
                notifyItemRangeRemoved(position, count);
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                notifyItemMoved(fromPosition, toPosition);
            }

            @Override
            public void onChanged(int position, int count) {
                notifyItemRangeChanged(position, count);
            }

            @Override
            public boolean areContentsTheSame(T oldItem, T newItem) {
                return SortedListAdapter.this.areItemContentsTheSame(oldItem, newItem);
            }

            @Override
            public boolean areItemsTheSame(T item1, T item2) {
                return SortedListAdapter.this.areItemsTheSame(item1, item2);
            }
        });
    }

    @Override
    public final ViewHolder<? extends T> onCreateViewHolder(ViewGroup parent, int viewType) {
        return onCreateViewHolder(mInflater, parent, viewType);
    }

    protected abstract ViewHolder<? extends T> onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType);

    protected abstract boolean areItemsTheSame(T item1, T item2);
    protected abstract boolean areItemContentsTheSame(T oldItem, T newItem);

    @Override
    public final void onBindViewHolder(ViewHolder<? extends T> holder, int position) {
        final T item = mSortedList.get(position);
        ((ViewHolder<T>) holder).bind(item);
    }

    @Override
    public void onBindViewHolder(ViewHolder<? extends T> holder, int position, List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
        final T item = mSortedList.get(position);
        if(payloads.size() > 0)
            ((ViewHolder<T>) holder).bindPartial(item, payloads);
    }

    public final Editor<T> edit() {
        return new EditorImpl();
    }

    public final T getItem(int position) {
        return mSortedList.get(position);
    }

    public final T getItem(T itemToFind) {
        int indexOf = getItemPosition(itemToFind);
        if(indexOf != -1) return mSortedList.get(indexOf);
        return null;
    }

    public final int getItemPosition(T itemToFind) {
        for (int i = 0; i < mSortedList.size(); ++i){
            if(mSortedList.get(i).equals(itemToFind)){
                return i;
            }
        }
        return -1;
    }

    @Override
    public final int getItemCount() {
        return mSortedList.size();
    }

    public final List<T> filter(Filter<T> filter) {
        final List<T> list = new ArrayList<>();
        for (int i = 0, count = mSortedList.size(); i < count; i++) {
            final T item = mSortedList.get(i);
            if (filter.test(item)) {
                list.add(item);
            }
        }
        return list;
    }

    public final T filterOne(Filter<T> filter) {
        for (int i = 0, count = mSortedList.size(); i < count; i++) {
            final T item = mSortedList.get(i);
            if (filter.test(item)) {
                return item;
            }
        }
        return null;
    }

    private interface Action<T extends ViewModel> {
        void perform(SortedList<T> list);
    }

    private class EditorImpl implements Editor<T> {

        private final List<Action<T>> mActions = new ArrayList<>();

        @Override
        public Editor<T> add(final T item) {
            mActions.add(new Action<T>() {
                @Override
                public void perform(SortedList<T> list) {
                    mSortedList.add(item);
                }
            });
            return this;
        }

        @Override
        public Editor<T> add(final List<T> items) {
            mActions.add(new Action<T>() {
                @Override
                public void perform(SortedList<T> list) {
                    Collections.sort(items, mComparator);
                    mSortedList.addAll(items);
                }
            });
            return this;
        }

        @Override
        public Editor<T> remove(final T item) {
            mActions.add(new Action<T>() {
                @Override
                public void perform(SortedList<T> list) {
                    mSortedList.remove(item);
                }
            });
            return this;
        }

        @Override
        public Editor<T> remove(final List<T> items) {
            mActions.add(new Action<T>() {
                @Override
                public void perform(SortedList<T> list) {
                    for (T item : items) {
                        mSortedList.remove(item);
                    }
                }
            });
            return this;
        }

        @Override
        public Editor<T> replaceAll(final List<T> items) {
            mActions.add(new Action<T>() {
                @Override
                public void perform(SortedList<T> list) {
                    // Changing this to update items that are already in the list
                    // so that the items don't disappear or appear to flash
                    final LongSparseArray<T> itemsToUpdate = new LongSparseArray<T>();
                    final List<T> matchItems = filterContains(items);
                    for (T item :
                            matchItems) {
                        int index = mSortedList.indexOf(item);
                        if(index > -1) {
                            itemsToUpdate.put(index, item);
                        }
                    }
                    for (int i = 0; i < itemsToUpdate.size(); ++i){
                        int key = (int) itemsToUpdate.keyAt(i);
                        T item = itemsToUpdate.get(key);
                        mSortedList.updateItemAt(key, item);
                        //since it is updating, remove from initial list
                        items.remove(item);
                    }
                    //add the remaining
                    if(items.size() > 0)
                        mSortedList.addAll(items);
                }
            });
            return this;
        }

        @Override
        public Editor<T> replace(final T item) {
            mActions.add(new Action<T>() {
                @Override
                public void perform(SortedList<T> list) {
                    int matchIndex = mSortedList.indexOf(item);
                    if(matchIndex > -1) {
                        mSortedList.updateItemAt(matchIndex, item);
                    }else{
                        mSortedList.add(item);
                    }
                }
            });
            return this;
        }

        @Override
        public Editor<T> removeAll() {
            mActions.add(new Action<T>() {
                @Override
                public void perform(SortedList<T> list) {
                    mSortedList.clear();
                }
            });
            return this;
        }

        @Override
        public void commit() {
            mSortedList.beginBatchedUpdates();
            for (Action<T> action : mActions) {
                action.perform(mSortedList);
            }
            mSortedList.endBatchedUpdates();
            mActions.clear();
        }

        @Override
        public List<T> filterContains(final List<T> items) {
            final List<T> itemsToRemove = filter(new Filter<T>() {
                @Override
                public boolean test(T item) {
                    return !items.contains(item);
                }
            });
            for (int i = itemsToRemove.size() - 1; i >= 0; i--) {
                final T item = itemsToRemove.get(i);
                items.remove(item);
            }
            return items;
        }

        @Override
        public List<T> filterFirst(final List<T> items) {
            final List<T> itemsToRemove = filter(new Filter<T>() {
                @Override
                public boolean test(T item) {
                    return items.contains(item);
                }
            });
            for (int i = itemsToRemove.size() - 1; i >= 0; i--) {
                final T item = itemsToRemove.get(i);
                items.remove(item);
            }
            return items;
        }

        @Override
        public List<T> filterContainsAndNew(List<T> items) {
            List<T> filter = filterContains(items);
            for (int i = filter.size() - 1; i >= 0; i--) {
                final T item = filter.get(i);
                final int indexOf = mSortedList.indexOf(item);
                if(indexOf > -1){
                    T toCompare = mSortedList.get(indexOf);
                    if(areItemContentsTheSame(item, toCompare)){
                        filter.remove(item);
                    }
                }
            }
            return filter;
        }

        @Override
        public Editor<T> removeByIndex(final int position, final int count) {
            mActions.add(new Action<T>() {
                @Override
                public void perform(SortedList<T> list) {
                    for (int i = position; i < position + count; ++i) {
                        mSortedList.removeItemAt(i);
                    }
                }
            });
            return this;
        }
    }

    public abstract static class ViewHolder<T extends ViewModel> extends RecyclerView.ViewHolder {

        private T mCurrentItem;

        public ViewHolder(View itemView) {
            super(itemView);
        }

        public final void bind(T item) {
            mCurrentItem = item;
            performBind(item);
        }

        protected abstract void performBind(T item);

        public final T getCurrentItem() {
            return mCurrentItem;
        }

        public void bindPartial(T item, List<Object> payloads) {
            mCurrentItem = item;
            performPartialBind(item, payloads);
        }

        protected abstract void performPartialBind(T item, List<Object> payloads);
    }

    public interface ViewModel {
    }
}
