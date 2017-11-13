package com.cardflight.sdk.sample;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.cardflight.sdk.CardReaderInfo;

/**
 * Created by radhikadayal on 11/2/17.
 */

public class ReadersAdapter extends RecyclerView.Adapter<ReadersAdapter.ReaderViewHolder> {

    private CardReaderInfo[] mReaderList;
    private CardReaderInfoClickListener mListener;

    interface CardReaderInfoClickListener {
        void onCardReaderInfoClicked(CardReaderInfo cardReaderInfo);
    }

    ReadersAdapter(CardReaderInfoClickListener cardReaderInfoClickListener) {
        mReaderList = new CardReaderInfo[0];
        mListener = cardReaderInfoClickListener;
    }

    void updateReaderList(CardReaderInfo[] cardReaderInfoArray) {
        mReaderList = cardReaderInfoArray;
        notifyDataSetChanged();
    }

    @Override
    public ReadersAdapter.ReaderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ReaderViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_reader, parent,
                        false));
    }

    @Override
    public void onBindViewHolder(ReaderViewHolder holder, int position) {
        final CardReaderInfo cardReaderInfo = mReaderList[position];
        holder.setName(cardReaderInfo.getName(), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onCardReaderInfoClicked(cardReaderInfo);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mReaderList.length;
    }

    class ReaderViewHolder extends RecyclerView.ViewHolder {

        private Button mReaderButton;

        ReaderViewHolder(View itemView) {
            super(itemView);

            mReaderButton = itemView.findViewById(R.id.button_name);
        }

        void setName(String name, View.OnClickListener onClickListener) {
            mReaderButton.setText(name);
            mReaderButton.setOnClickListener(onClickListener);
        }
    }
}
